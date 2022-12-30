package com.nextsquad.house.service;

import com.nextsquad.house.domain.house.*;
import com.nextsquad.house.domain.user.User;
import com.nextsquad.house.dto.GeneralResponse;
import com.nextsquad.house.dto.SearchCondition;
import com.nextsquad.house.dto.bookmark.BookmarkRequest;
import com.nextsquad.house.dto.rentarticle.RentArticleCreationResponse;
import com.nextsquad.house.dto.rentarticle.RentArticleListResponse;
import com.nextsquad.house.dto.rentarticle.RentArticleRequest;
import com.nextsquad.house.dto.rentarticle.RentArticleResponse;
import com.nextsquad.house.exception.*;
import com.nextsquad.house.login.jwt.JwtProvider;
import com.nextsquad.house.repository.rentarticle.*;
import com.nextsquad.house.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachePut;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class RentArticleService {
    private final RedisService redisService;
    private final RentArticleRepository rentArticleRepository;
    private final UserRepository userRepository;
    private final HouseImageRepository houseImageRepository;
    private final RentArticleBookmarkRepository rentArticleBookmarkRepository;
    private final HouseFacilityRepository houseFacilityRepository;
    private final JwtProvider jwtProvider;
    private final CacheManager cacheManager;
    private final RentArticleDocumentRepository rentArticleDocumentRepository;

    public RentArticleCreationResponse writeRentArticle(RentArticleRequest request, String token) {
        User user = getUserFromAccessToken(token);

        List<String> houseImageUrls = request.getHouseImages();
        HouseFacility houseFacility = request.extractHouseFacility();
        houseFacilityRepository.save(houseFacility);

        RentArticle rentArticle = generateRentArticle(request, user, houseFacility);
        rentArticleRepository.save(rentArticle);

        saveHouseImage(rentArticle, houseImageUrls);

        return new RentArticleCreationResponse(rentArticle.getId());
    }

//    @Cacheable(value = "rentArticle", key = "#searchCondition + ';' + #pageable", condition = "#count > 5")
    public RentArticleListResponse getRentArticles(SearchCondition searchCondition, Pageable pageable, String token, Integer count) {
        List<RentArticleBookmark> listByUser = rentArticleBookmarkRepository.findByUserId(getUserIdFromAccessToken(token));
        Map<Long, Boolean> bookmarkHashMap = getBookmarkedArticleMap(listByUser);

        List<RentArticle> rentArticles = getArticlesFromDocuments(searchCondition, pageable);
        boolean hasNext = checkHasNext(pageable, rentArticles);

        return RentArticleListResponse.of(rentArticles, bookmarkHashMap, hasNext);
    }

    private List<RentArticle> getArticlesFromDocuments(SearchCondition searchCondition, Pageable pageable) {
        List<RentArticleDocument> documents = rentArticleDocumentRepository.findByTitle(searchCondition, pageable);
        List<Long> ids = documents.stream().map(RentArticleDocument::getId).collect(Collectors.toList());
        return rentArticleRepository.findAllById(ids);
    }

    @CachePut(value = "cacheCount", key = "#searchCondition + ';' + #pageable")
    public int getCacheCount(SearchCondition searchCondition, Pageable pageable) {
        Cache cache = Optional.ofNullable(cacheManager.getCache("cacheCount")).orElseThrow(() -> new IllegalStateException("해당하는 redis 캐시가 존재하지 않습니다."));
        Integer count = Optional.ofNullable(cache.get(searchCondition, Integer.class)).orElse(0);
        cache.put(searchCondition, ++count);
        return count;
    }


    public RentArticleResponse generateRentArticle(Long id, String token) {
        RentArticle rentArticle = rentArticleRepository.findById(id).orElseThrow(ArticleNotFoundException::new);
        if (rentArticle.isDeleted() || rentArticle.isCompleted()) {
            throw new IllegalArgumentException("삭제되었거나 거래가 완료된 글입니다.");
        }

        rentArticle.addViewCount();

        User user = getUserFromAccessToken(token);

        boolean isBookmarked = rentArticleBookmarkRepository.findByUserAndRentArticle(user, rentArticle).isPresent();

        return new RentArticleResponse(rentArticle, isBookmarked);
    }

    public GeneralResponse toggleIsCompleted(Long id, String token) {
        RentArticle rentArticle = rentArticleRepository.findById(id)
                .orElseThrow(ArticleNotFoundException::new);

        authorizeArticleOwner(token, rentArticle);

        rentArticle.toggleIsCompleted();
        return new GeneralResponse(200, "게시글 상태가 변경되었습니다.");
    }

    public GeneralResponse deleteArticle(Long id, String token) {
        RentArticle rentArticle = rentArticleRepository.findById(id)
                .orElseThrow(ArticleNotFoundException::new);

        authorizeArticleOwner(token, rentArticle);

        rentArticle.markAsDeleted();
        rentArticleBookmarkRepository.deleteByRentArticle(rentArticle);
        return new GeneralResponse(200, "게시글이 삭제되었습니다.");
    }

    public GeneralResponse addBookmark(BookmarkRequest request, String token) {
        User user = getUserFromAccessToken(token);
        log.info("user id: {}", user.getId());
        RentArticle rentArticle = rentArticleRepository.findById(request.getArticleId()).orElseThrow(ArticleNotFoundException::new);
        redisService.increment("rentBookmarkCount::" + rentArticle.getId());

        if (rentArticleBookmarkRepository.findByUserAndRentArticle(user, rentArticle).isPresent()) {
            throw new DuplicateBookmarkException();
        }

        checkIsAvailable(rentArticle);
        rentArticleBookmarkRepository.save(new RentArticleBookmark(rentArticle, user));
        return new GeneralResponse(200, "북마크에 추가 되었습니다.");
    }

    @Scheduled(cron = "0 0/3 * * * ?")
    public void SynchronizeRedisCountToDatabase() {
        Set<String> keys = redisService.getKeys("rentBookmarkCount*");

        for (String key : keys) {
            long id = Long.parseLong(key.split("::")[1]);
            int count = Integer.parseInt(redisService.get(key));

            RentArticle article = rentArticleRepository.findById(id)
                    .orElseThrow(ArticleNotFoundException::new);

            article.setBookmarkCount(article.getBookmarkCount() + count);

            redisService.delete(key);
        }
    }

    public GeneralResponse deleteBookmark(BookmarkRequest request, String token) {
        User user = getUserFromAccessToken(token);

        RentArticle rentArticle = rentArticleRepository.findById(request.getArticleId())
                .orElseThrow(ArticleNotFoundException::new);
        RentArticleBookmark bookmark = rentArticleBookmarkRepository.findByUserAndRentArticle(user, rentArticle)
                .orElseThrow(BookmarkNotFoundException::new);
        rentArticleBookmarkRepository.delete(bookmark);
        redisService.decrement("rentBookmarkCount::" + rentArticle.getId());
        return new GeneralResponse(200, "북마크가 삭제되었습니다.");
    }

    public GeneralResponse modifyRentArticle(Long id, RentArticleRequest request, String token) {
        log.info("updating {}... ", request.getTitle());
        RentArticle rentArticle = rentArticleRepository.findById(id)
                .orElseThrow(ArticleNotFoundException::new);

        authorizeArticleOwner(token, rentArticle);

        rentArticle.getHouseFacility().updateHouseFacility(request.extractHouseFacility());

        houseImageRepository.deleteAllByArticle(rentArticle);
        saveHouseImage(rentArticle, request.getHouseImages());

        rentArticle.modifyArticle(request);

        return new GeneralResponse(200, "게시글이 수정되었습니다.");
    }

    private void checkIsAvailable(RentArticle rentArticle) {
        if (rentArticle.isDeleted()) {
            throw new IllegalArgumentException("삭제된 게시글은 추가할 수 없습니다.");
        }
        if (rentArticle.isCompleted()) {
            throw new IllegalArgumentException("삭제된 게시글은 추가할 수 없습니다.");
        }
    }

    private boolean checkHasNext(Pageable pageable, List<RentArticle> rentArticles) {
        boolean checkHasNext = pageable.getPageSize() < rentArticles.size();
        if (checkHasNext) {
            rentArticles.remove(rentArticles.size() - 1);
        }
        return checkHasNext;
    }

    private Map<Long, Boolean> getBookmarkedArticleMap(List<RentArticleBookmark> listByUser) {
        Map<Long, Boolean> bookmarkHashMap = new HashMap<>();
        for (RentArticleBookmark rentArticleBookmark : listByUser) {
            bookmarkHashMap.put(rentArticleBookmark.getRentArticle().getId(), true);
        }
        return bookmarkHashMap;
    }

    private User getUserFromAccessToken(String token) {
        Long id = jwtProvider.decode(token).getClaim("id").asLong();
        return userRepository.findById(id).orElseThrow(UserNotFoundException::new);
    }

    private Long getUserIdFromAccessToken(String token) {
        return jwtProvider.decode(token).getClaim("id").asLong();
    }

    private void saveHouseImage(RentArticle rentArticle, List<String> houseImageUrls) {
        for (int i = 0; i < houseImageUrls.size(); i++) {
            houseImageRepository.save(new HouseImage(houseImageUrls.get(i), rentArticle, i));
        }
    }

    private void authorizeArticleOwner(String token, RentArticle article) {
        Long loggedInId = jwtProvider.decode(token).getClaim("id").asLong();
        User user = userRepository.findById(loggedInId)
                .orElseThrow(UserNotFoundException::new);

        if (!user.equals(article.getUser())) {
            throw new AccessDeniedException();
        }
    }

    private RentArticle generateRentArticle(RentArticleRequest request, User user, HouseFacility houseFacility) {
        return RentArticle.builder()
                .user(user)
                .title(request.getTitle())
                .houseType(HouseType.valueOf(request.getHouseType()))
                .rentFee(request.getRentFee())
                .deposit(request.getDeposit())
                .availableFrom(request.getAvailableFrom())
                .contractExpiresAt(request.getContractExpiresAt())
                .maintenanceFee(request.getMaintenanceFee())
                .maintenanceFeeDescription(request.getMaintenanceFeeDescription())
                .address(request.getAddress())
                .addressDetail(request.getAddressDetail())
                .addressDescription(request.getAddressDescription())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .houseFacility(houseFacility)
                .content(request.getContent())
                .contractType(ContractType.valueOf(request.getContractType()))
                .maxFloor(request.getMaxFloor())
                .thisFloor(request.getThisFloor())
                .createdAt(LocalDateTime.now())
                .modifiedAt(LocalDateTime.now())
                .build();
    }
}

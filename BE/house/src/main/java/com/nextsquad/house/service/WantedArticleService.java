package com.nextsquad.house.service;

import com.nextsquad.house.domain.house.WantedArticle;
import com.nextsquad.house.domain.user.User;
import com.nextsquad.house.dto.wantedArticle.WantedArticleElementResponse;
import com.nextsquad.house.dto.wantedArticle.SavedWantedArticleResponse;
import com.nextsquad.house.dto.wantedArticle.WantedArticleListResponse;
import com.nextsquad.house.dto.wantedArticle.WantedArticleRequest;
import com.nextsquad.house.dto.wantedArticle.WantedArticleResponse;
import com.nextsquad.house.repository.UserRepository;
import com.nextsquad.house.repository.WantedArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WantedArticleService {

    private final WantedArticleRepository wantedArticleRepository;
    private final UserRepository userRepository;

    public SavedWantedArticleResponse writeWantedArticle(WantedArticleRequest request) {
        User user = userRepository.findById(request.getUserId()).orElseThrow();
        WantedArticle wantedArticle = WantedArticle.builder()
                .user(user)
                .address(request.getAddress())
                .title(request.getTitle())
                .content(request.getContent())
                .moveInDate(request.getMoveInDate())
                .moveOutDate(request.getMoveOutDate())
                .rentBudget(request.getRentBudget())
                .depositBudget(request.getDepositBudget())
                .createdAt(LocalDateTime.now())
                .modifiedAt(LocalDateTime.now())
                .build();

        wantedArticleRepository.save(wantedArticle);
        return new SavedWantedArticleResponse(wantedArticle.getId());
    }


    public WantedArticleResponse getWantedArticle(Long articleId) {
        WantedArticle article = wantedArticleRepository.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("요청하신 id에 해당하는 게시글이 없습니다."));
        return WantedArticleResponse.from(article);
    }
    
    public WantedArticleListResponse getWantedArticleList() {
        List<WantedArticleElementResponse> elementResponseList = wantedArticleRepository.findByAvailable()
                .stream().map(WantedArticleElementResponse::from).collect(Collectors.toList());
        return new WantedArticleListResponse(elementResponseList);
    }
}

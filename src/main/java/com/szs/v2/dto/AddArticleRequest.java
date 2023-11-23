package com.szs.v2.dto;

import com.szs.v2.domain.Article;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class AddArticleRequest {

    private String title;
    private String content;

    public Article toEntity() {

        // 빌더 패턴을 사용해 DTO를 엔티티로 만들어주는 메서드.
        // 이 메서드는 추후에 블로그 글을 저장할 엔티티로 변환하는 용도로 사용한다.

        return Article.builder()
                .title(title)
                .content(content)
                .build();
    }
}

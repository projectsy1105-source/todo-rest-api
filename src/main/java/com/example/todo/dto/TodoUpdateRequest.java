package com.example.todo.dto;

import jakarta.validation.constraints.AssertTrue;

public record TodoUpdateRequest(String title, Boolean completed) {
    @AssertTrue(message = "수정할 title 또는 completed 값이 필요합니다.")
    public boolean hasUpdateField() {
        return title != null || completed != null;
    }

    @AssertTrue(message = "제목은 공백이 아닌 1~200자여야 합니다.")
    public boolean isTitleValid() {
        return title == null || (!title.isBlank() && title.length() <= 200);
    }
}

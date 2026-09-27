package com.example.todo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TodoCreateRequest(
        @NotBlank(message = "제목은 공백이 아닌 1~200자여야 합니다.")
        @Size(max = 200, message = "제목은 공백이 아닌 1~200자여야 합니다.")
        String title
) {
}

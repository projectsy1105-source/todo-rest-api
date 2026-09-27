package com.example.todo.service;

import com.example.todo.common.InvalidTodoRequestException;
import com.example.todo.common.TodoNotFoundException;
import com.example.todo.dto.TodoCreateRequest;
import com.example.todo.dto.TodoResponse;
import com.example.todo.dto.TodoUpdateRequest;
import com.example.todo.entity.Todo;
import com.example.todo.repository.TodoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TodoService {
    private final TodoRepository todoRepository;

    public TodoService(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }

    @Transactional
    public TodoResponse create(TodoCreateRequest request) {
        validateTitle(request.title());
        Todo todo = todoRepository.save(new Todo(request.title()));
        return TodoResponse.from(todo);
    }

    @Transactional(readOnly = true)
    public List<TodoResponse> findAll() {
        return todoRepository.findAll().stream().map(TodoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TodoResponse findById(Long id) {
        return TodoResponse.from(findTodo(id));
    }

    @Transactional
    public TodoResponse update(Long id, TodoUpdateRequest request) {
        if (request.title() != null) {
            validateTitle(request.title());
        }
        Todo todo = findTodo(id);
        todo.update(request.title(), request.completed());
        return TodoResponse.from(todo);
    }

    @Transactional
    public void delete(Long id) {
        todoRepository.delete(findTodo(id));
    }

    private Todo findTodo(Long id) {
        return todoRepository.findById(id).orElseThrow(() -> new TodoNotFoundException(id));
    }

    private void validateTitle(String title) {
        if (title == null || title.isBlank() || title.length() > 200) {
            throw new InvalidTodoRequestException("제목은 공백이 아닌 1~200자여야 합니다.");
        }
    }
}

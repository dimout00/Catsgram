package ru.yandex.practicum.catsgram.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.catsgram.exception.ConditionsNotMetException;
import ru.yandex.practicum.catsgram.exception.NotFoundException;
import ru.yandex.practicum.catsgram.model.Post;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {
    private final Map<Long, Post> posts = new HashMap<>();
    private final UserService userService;

    public Collection<Post> findAll(Integer from, Integer size, SortOrder sortOrder) {
        // Значения по умолчанию
        if (from == null) from = 0;
        if (size == null) size = 10;
        if (sortOrder == null) sortOrder = SortOrder.DESCENDING;

        // Проверка корректности параметров
        if (from < 0) {
            throw new ConditionsNotMetException("Параметр 'from' не может быть отрицательным");
        }
        if (size <= 0) {
            throw new ConditionsNotMetException("Параметр 'size' должен быть положительным");
        }

        List<Post> sortedPosts = new ArrayList<>(posts.values());

        // Сортировка по дате
        if (sortOrder == SortOrder.ASCENDING) {
            sortedPosts.sort(Comparator.comparing(Post::getPostDate));
        } else {
            sortedPosts.sort(Comparator.comparing(Post::getPostDate).reversed());
        }

        // Пагинация
        return sortedPosts.stream()
                .skip(from)
                .limit(size)
                .collect(Collectors.toList());
    }

    // Старый метод оставляем для обратной совместимости
    public Collection<Post> findAll() {
        return findAll(0, 10, SortOrder.DESCENDING);
    }

    public Optional<Post> findPostById(Long postId) {
        return Optional.ofNullable(posts.get(postId));
    }

    public Post create(Post post) {
        // Проверка описания
        if (post.getDescription() == null || post.getDescription().isBlank()) {
            throw new ConditionsNotMetException("Описание не может быть пустым");
        }

        // Проверка существования автора
        if (post.getAuthorId() == null) {
            throw new ConditionsNotMetException("Идентификатор автора не может быть пустым");
        }

        userService.findUserById(post.getAuthorId())
                .orElseThrow(() -> new ConditionsNotMetException(
                        "Автор с id = " + post.getAuthorId() + " не найден"
                ));

        post.setId(getNextId());
        post.setPostDate(Instant.now());
        posts.put(post.getId(), post);
        return post;
    }

    public Post update(Post newPost) {
        if (newPost.getId() == null) {
            throw new ConditionsNotMetException("Id должен быть указан");
        }
        if (posts.containsKey(newPost.getId())) {
            Post oldPost = posts.get(newPost.getId());
            if (newPost.getDescription() == null || newPost.getDescription().isBlank()) {
                throw new ConditionsNotMetException("Описание не может быть пустым");
            }
            oldPost.setDescription(newPost.getDescription());
            return oldPost;
        }
        throw new NotFoundException("Пост с id = " + newPost.getId() + " не найден");
    }

    private long getNextId() {
        long currentMaxId = posts.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }
}
---
name: testing
description: How to write tests for this chess backend project
---

# Testing Skill

## Test Structure
```
src/test/java/com/chessapp/server/
├── application/service/    → Unit tests for services (Mockito)
├── presentation/rest/      → Controller integration tests (@WebMvcTest)
└── presentation/websocket/ → WebSocket integration tests
```

## Unit Tests (Services)
- Use `@ExtendWith(MockitoExtension.class)`
- Mock ALL dependencies with `@Mock`
- Inject the class under test with `@InjectMocks`
- Test happy path + edge cases + error cases

```java
@ExtendWith(MockitoExtension.class)
class FriendServiceImplTest {
    @Mock private FriendshipRepository friendshipRepository;
    @Mock private UserService userService;
    @InjectMocks private FriendServiceImpl friendService;

    @Test
    void sendRequest_shouldCreateFriendship() {
        // given
        // when
        // then (verify + assert)
    }
}
```

## Controller Tests
- Use `@WebMvcTest(ControllerClass.class)`
- Mock service layer with `@MockBean`
- Use `MockMvc` for HTTP requests

## Naming Convention
- Test class: `{ClassName}Test.java`
- Test method: `{methodName}_{scenario}` or `{methodName}_should{ExpectedBehavior}`

## What to Test
- Service methods: validation, business rules, edge cases
- Controllers: request/response mapping, status codes, error handling
- NEVER test: JPA repositories (tested by Spring), private methods directly

package com.hoz.hozitech.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hoz.hozitech.application.repositories.UserRepository;
import com.hoz.hozitech.domain.entities.User;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LoginIdentifierResolverTest {

    @Test
    void resolvesExistingUserByEmailOrUsernameFirst() {
        User user = User.builder()
                .email("alice@example.com")
                .userName("alice")
                .build();

        UserRepository userRepository = createRepository(
                Map.of("alice@example.com", user),
                Map.of());

        LoginIdentifierResolver loginIdentifierResolver = new LoginIdentifierResolver(userRepository);

        Optional<User> resolved = loginIdentifierResolver.resolve("alice@example.com");

        assertTrue(resolved.isPresent());
        assertEquals("alice@example.com", resolved.get().getEmail());
    }

    @Test
    void resolvesVietnamesePhoneNumbersAcrossZeroAndCountryCodeFormats() {
        User user = User.builder()
                .email("alice@example.com")
                .userName("alice")
                .phoneNumber("0912345678")
                .build();

        UserRepository userRepository = createRepository(
                Map.of(),
                Map.of("0912345678", user));

        LoginIdentifierResolver loginIdentifierResolver = new LoginIdentifierResolver(userRepository);

        Optional<User> resolved = loginIdentifierResolver.resolve("+84 912 345 678");

        assertTrue(resolved.isPresent());
        assertEquals("0912345678", resolved.get().getPhoneNumber());
    }

    private UserRepository createRepository(
            Map<String, User> emailOrUsernameUsers,
            Map<String, User> phoneUsers) {
        Map<String, User> lookupByIdentity = new HashMap<>(emailOrUsernameUsers);

        return (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[]{UserRepository.class},
                (proxy, method, args) -> {
                    String methodName = method.getName();

                    if ("findByEmailOrUserName".equals(methodName)) {
                        return Optional.ofNullable(lookupByIdentity.get((String) args[0]));
                    }

                    if ("findByPhoneNumber".equals(methodName)) {
                        return Optional.ofNullable(phoneUsers.get((String) args[0]));
                    }

                    if ("equals".equals(methodName)) {
                        return proxy == args[0];
                    }

                    if ("hashCode".equals(methodName)) {
                        return System.identityHashCode(proxy);
                    }

                    if ("toString".equals(methodName)) {
                        return "TestUserRepository";
                    }

                    Class<?> returnType = method.getReturnType();
                    if (returnType.equals(boolean.class)) {
                        return false;
                    }
                    if (returnType.equals(long.class)) {
                        return 0L;
                    }
                    if (returnType.equals(int.class)) {
                        return 0;
                    }
                    if (Optional.class.equals(returnType)) {
                        return Optional.empty();
                    }
                    return null;
                });
    }
}

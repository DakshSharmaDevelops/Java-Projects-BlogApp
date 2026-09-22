package com.blog.securityConfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/login/**", "/register", "/style.css").permitAll()
                        .requestMatchers(HttpMethod.GET, "/post/new").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.GET, "/post/edit/**").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.GET, "/post/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/post/save").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.POST, "/post/delete/**").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.POST, "/post/ai-suggest").hasAnyRole("ADMIN", "USER")
                        .requestMatchers(HttpMethod.POST, "/comments/**").authenticated()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(loginSuccessHandler())
                        .failureHandler((request, response, exception) -> {
                            String type = "admin".equals(request.getParameter("loginType")) ? "admin" : "user";
                            response.sendRedirect("/login/" + type + "?error=true");
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public UserDetailsService userDetailsService(com.blog.service.UserAccountService userAccountService) {
        PasswordEncoder passwordEncoder = passwordEncoder();
        return username -> {
            if ("admin".equalsIgnoreCase(username)) {
                return User.withUsername("admin")
                        .password(passwordEncoder.encode("admin123"))
                        .roles("ADMIN")
                        .build();
            }
            return userAccountService.loadUserByUsername(username);
        };
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService,
                                                            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationSuccessHandler loginSuccessHandler() {
        return (request, response, authentication) -> {
            String loginType = request.getParameter("loginType");
            boolean adminLogin = "admin".equals(loginType);
            boolean correctRole = hasRole(authentication, adminLogin ? "ROLE_ADMIN" : "ROLE_USER");

            if (!correctRole) {
                SecurityContextHolder.clearContext();
                request.getSession().invalidate();
                response.sendRedirect("/login/" + (adminLogin ? "admin" : "user") + "?error=role");
                return;
            }

            response.sendRedirect(adminLogin ? "/" : "/");
        };
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> role.equals(authority.getAuthority()));
    }
}
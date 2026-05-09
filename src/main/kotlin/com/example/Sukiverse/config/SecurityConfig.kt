package com.example.Sukiverse.config

import com.example.Sukiverse.config.oauth2.CustomOAuth2UserService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val customOAuth2UserService: CustomOAuth2UserService,
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { auth ->
                auth.requestMatchers("/", "/login/**", "/oauth2/**", "/images/**").permitAll()
                auth.anyRequest().authenticated()
            }
            .oauth2Login { oauth2 ->
                oauth2.userInfoEndpoint { it.userService(customOAuth2UserService) }
                oauth2.defaultSuccessUrl("/welcome")
            }
            .exceptionHandling { exceptions ->
                exceptions.authenticationEntryPoint(LoginUrlAuthenticationEntryPoint("/login"))
            }
            .logout { logout ->
                logout.logoutUrl("/logout")
                logout.logoutSuccessUrl("/login")
                logout.deleteCookies("JSESSIONID")
                logout.invalidateHttpSession(true)
            }
            .csrf { it.disable() }

        return http.build()
    }
}

package com.example.Sukiverse.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { auth ->
                auth.requestMatchers("/", "/login/**", "/oauth2/**", "/images/**").permitAll()
                auth.anyRequest().authenticated()
            }
            .oauth2Login { oauth2 ->
                oauth2.defaultSuccessUrl("/login/complete")
            }
            .headers { headers ->
                headers.frameOptions { it.disable() }
            }
            .exceptionHandling { exceptions ->
                exceptions.authenticationEntryPoint(LoginUrlAuthenticationEntryPoint("/login"))
            }
            .formLogin { form ->
                form.successForwardUrl("/welcome")
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

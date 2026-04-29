package com.example.Sukiverse.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody

@Controller
class LoginController {

    @GetMapping("/")
    fun index() = "redirect:/login"

    @GetMapping("/login")
    fun login() = "login"

    @GetMapping("/login/complete")
    fun loginComplete() = "redirect:/welcome"

    @GetMapping("/welcome")
    @ResponseBody
    fun welcome() = "Hello! Social Login!!"
}

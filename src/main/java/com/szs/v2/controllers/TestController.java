package com.szs.v2.controllers;

import com.szs.v2.domain.Member;
import com.szs.v2.services.TestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/test")
@Tag(name = "Template", description = "템플릿 API Document")
public class TestController {

    @Autowired
    TestService testService;

    @GetMapping("/")
    @Operation(summary = "템플릿 리스트", description = "템플릿의 모든 리스트를 조회합니다.")
    public String test() {
        return "Hello, world!";
    }

    @GetMapping("/test")
    public List<Member> getAllmembers() {
        List<Member> members = testService.getAllMembers();
        return members;
    }
}
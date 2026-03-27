package com.fullcount.controller;

import com.fullcount.repository.MemberRepository;
import com.fullcount.repository.PostRepository;
import com.fullcount.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final TransferRepository transferRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("memberCount", memberRepository.count());
        model.addAttribute("postCount", postRepository.count());
        model.addAttribute("transferCount", transferRepository.count());
        return "admin/dashboard";
    }

    @GetMapping("/members")
    public String members(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Model model) {
        model.addAttribute("members", memberRepository.findAll(pageable));
        return "admin/members";
    }

    @GetMapping("/transfers")
    public String transfers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Model model) {
        model.addAttribute("transfers", transferRepository.findAll(pageable));
        return "admin/transfers";
    }
}

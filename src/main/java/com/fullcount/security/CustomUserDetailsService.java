package com.fullcount.security;

import com.fullcount.domain.Member;
import com.fullcount.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 회원입니다."));

        return new User(
                member.getEmail(),
                member.getPassword(),
                member.getIsActive(),
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_" + member.getRole().name()))
        );
    }
}
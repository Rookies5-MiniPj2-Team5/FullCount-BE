package com.fullcount.config;

import com.fullcount.domain.Member;
import com.fullcount.domain.MemberRole;
import com.fullcount.domain.Team;
import com.fullcount.repository.MemberRepository;
import com.fullcount.repository.TeamRepository;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final TeamRepository teamRepository;
    private final PasswordEncoder passwordEncoder;
    private final DataSource dataSource;

    @Override
    public void run(String... args) throws Exception {
        initializeTeamsIfEmpty();

        String adminEmail = "admin22@fullcount.com";
        
        if (!memberRepository.existsByEmail(adminEmail)) {
            log.info("Creating default admin account: {}", adminEmail);
            
            // 기본 팀 하나 선택 (기존 데이터에 1번 팀이 있다고 가정)
            Team team = teamRepository.findById(1L).orElse(null);
            
            Member admin = Member.builder()
                    .email(adminEmail)
                    .nickname("admin22")
                    .password(passwordEncoder.encode("pwd1"))
                    .role(MemberRole.ADMIN)
                    .team(team)
                    .build();
            
            memberRepository.save(admin);
            log.info("Admin account created successfully.");
        } else {
            log.info("Admin account already exists.");
        }
    }

    private void initializeTeamsIfEmpty() {
        if (teamRepository.count() > 0) {
            log.info("Team data already exists. Skipping data.sql execution.");
            return;
        }

        log.info("Team data not found. Executing data.sql.");
        ResourceDatabasePopulator databasePopulator =
                new ResourceDatabasePopulator(new ClassPathResource("data.sql"));
        databasePopulator.execute(dataSource);
        log.info("Team data initialized from data.sql.");
    }
}

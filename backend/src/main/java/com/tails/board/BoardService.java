package com.tails.board;

import com.tails.board.dto.BoardCreateRequest;
import com.tails.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 게시글 관련 비즈니스 로직 
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public Long create(Long memberId, BoardCreateRequest request) {
        Board board = Board.builder()
                .member(memberRepository.getReferenceById(memberId))
                .title(request.title())
                .content(request.content())
                .build();
        return boardRepository.save(board).getId();
    }
}

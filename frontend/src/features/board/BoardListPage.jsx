// 게시글 목록 페이지, 검색과 정렬 지원
import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { getBoards } from './api';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../hooks/useToast';
import Pagination from '../../components/Pagination/Pagination';
import Button from '../../components/Button/Button';
import { resolveImage } from '../../utils/resolveImage';
import { HeartIcon, ChatBubbleIcon, PawIcon } from '../../components/Icon/Icon';
import styles from './BoardListPage.module.scss';

export default function BoardListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const page = Number(searchParams.get('page') ?? 0);
  const keyword = searchParams.get('keyword') ?? '';
  const sortBy = searchParams.get('sortBy') ?? '';
  const [keywordInput, setKeywordInput] = useState(keyword);
  const [boardPage, setBoardPage] = useState(null);
  const { isAuthenticated } = useAuth();
  const { showToast } = useToast();

  useEffect(() => {
    function fetchBoards() {
      getBoards({ page, keyword: keyword || undefined, sortBy: sortBy || undefined }).then((res) => {
        setBoardPage(res.data.data);
      });
    }
    fetchBoards();

    // 다른 탭에서 글을 쓰고 돌아왔을 때도 목록을 최신 상태로 갱신
    window.addEventListener('focus', fetchBoards);
    return () => window.removeEventListener('focus', fetchBoards);
  }, [page, keyword, sortBy]);

  function updateParams(next) {
    const params = { keyword, sortBy, page: 0, ...next };
    const cleaned = Object.fromEntries(Object.entries(params).filter(([, v]) => v !== '' && v != null));
    setSearchParams(cleaned);
  }

  function handleSearchSubmit(e) {
    e.preventDefault();
    updateParams({ keyword: keywordInput });
  }

  return (
    <div className={styles.wrapper}>
      <div className={styles.header}>
        <h1>피드</h1>
        <div className={styles.headerActions}>
          {isAuthenticated && (
            <Link to="/boards/drafts" className={styles.draftLink}>
              내 임시저장
            </Link>
          )}
          {isAuthenticated ? (
            <Link to="/boards/new">
              <Button>글쓰기</Button>
            </Link>
          ) : (
            <Button onClick={() => showToast('로그인이 필요합니다.', 'error')}>글쓰기</Button>
          )}
        </div>
      </div>

      <div className={styles.toolbar}>
        <form onSubmit={handleSearchSubmit} className={styles.searchForm}>
          <input
            type="text"
            placeholder="제목/내용 검색"
            value={keywordInput}
            onChange={(e) => setKeywordInput(e.target.value)}
          />
          <Button type="submit" variant="secondary">
            검색
          </Button>
        </form>
        <div className={styles.sortTabs}>
          <button
            className={sortBy !== 'popular' ? styles.active : ''}
            onClick={() => updateParams({ sortBy: '' })}
          >
            최신순
          </button>
          <button
            className={sortBy === 'popular' ? styles.active : ''}
            onClick={() => updateParams({ sortBy: 'popular' })}
          >
            인기순
          </button>
        </div>
      </div>

      {boardPage && (
        <>
          <ul className={styles.list}>
            {boardPage.content.map((board) => {
              const thumbnailSrc = resolveImage(board.thumbnailUrl);
              const createdAt = new Date(board.createdAt);

              const dateLabel = `${createdAt.getFullYear()}.${createdAt.getMonth() + 1}.${createdAt.getDate()}`;
              return (
              <li key={board.boardId}>
                <Link to={`/boards/${board.boardId}`} className={styles.item}>
                  {thumbnailSrc ? (
                    <img className={styles.thumbnail} src={thumbnailSrc} alt="" />
                  ) : (
                    <div className={styles.thumbnailPlaceholder}>
                      <PawIcon />
                    </div>
                  )}
                  <div className={styles.itemBody}>
                    <span className={styles.title}>{board.title}</span>
                    <p className={styles.excerpt}>{board.excerpt}</p>
                    <span className={styles.meta}>
                      {dateLabel} · 조회{board.viewCount} · 댓글{board.commentCount}
                    </span>
                    <div className={styles.bottomRow}>
                      <div className={styles.author}>
                        <span className={styles.authorAvatar}>
                          {resolveImage(board.authorProfileImg) ? (
                            <img src={resolveImage(board.authorProfileImg)} alt="" />
                          ) : (
                            board.authorNickname.slice(0, 1)
                          )}
                        </span>
                        <span className={styles.authorName}>{board.authorNickname}</span>
                      </div>
                      <span className={styles.likeBadge}>
                        <HeartIcon /> {board.likeCount}
                      </span>
                    </div>
                  </div>
                </Link>
              </li>
              );
            })}
            {boardPage.content.length === 0 && (
              <div className={styles.empty}>
                <p className={styles.emptyIcon}><ChatBubbleIcon /></p>
                <p>아직 올라온 이야기가 없어요.</p>
              </div>
            )}
          </ul>
          <Pagination
            page={boardPage.number}
            totalPages={boardPage.totalPages}
            onPageChange={(nextPage) => updateParams({ page: nextPage })}
          />
        </>
      )}
    </div>
  );
}

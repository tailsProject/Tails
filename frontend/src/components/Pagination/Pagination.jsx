// 공통 페이지네이션, 페이지 번호 10개 단위 블록 이동
import styles from './Pagination.module.scss';

const PAGE_BLOCK_SIZE = 10;

export default function Pagination({ page, totalPages, onPageChange }) {
  if (totalPages <= 1) {
    return null;
  }

  const currentBlock = Math.floor(page / PAGE_BLOCK_SIZE);
  const blockStart = currentBlock * PAGE_BLOCK_SIZE;
  const blockEnd = Math.min(blockStart + PAGE_BLOCK_SIZE, totalPages);

  const pageNumbers = [];
  for (let i = blockStart; i < blockEnd; i++) {
    pageNumbers.push(i);
  }

  const hasPrevBlock = blockStart > 0;
  const hasNextBlock = blockEnd < totalPages;

  return (
    <nav className={styles.pagination}>
      {hasPrevBlock && <button onClick={() => onPageChange(blockStart - 1)}>이전</button>}

      {pageNumbers.map((num) => (
        <button
          key={num}
          className={num === page ? styles.active : ''}
          onClick={() => onPageChange(num)}
        >
          {num + 1}
        </button>
      ))}

      {hasNextBlock && <button onClick={() => onPageChange(blockEnd)}>다음</button>}
    </nav>
  );
}

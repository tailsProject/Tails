// 빈 목록, 에러 등 상태 안내용 공통 컴포넌트
import { Link } from 'react-router-dom';
import styles from './StateMessage.module.scss';

export default function StateMessage({ icon: Icon, title, description, actionTo, actionLabel, onAction }) {
  return (
    <div className={styles.wrapper}>
      {Icon && (
        <span className={styles.iconBadge}>
          <Icon />
        </span>
      )}
      {title && <p className={styles.title}>{title}</p>}
      {description && <p className={styles.description}>{description}</p>}
      {actionTo && (
        <Link to={actionTo} className={styles.action}>
          {actionLabel}
        </Link>
      )}
      {onAction && (
        <button type="button" className={styles.action} onClick={onAction}>
          {actionLabel}
        </button>
      )}
    </div>
  );
}

// 공통 버튼 컴포넌트
import styles from './Button.module.scss';

export default function Button({ variant = 'primary', className = '', children, ...props }) {
  return (
    <button className={`${styles.button} ${styles[variant]} ${className}`} {...props}>
      {children}
    </button>
  );
}

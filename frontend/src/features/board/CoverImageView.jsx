// 에디터 안 이미지 노드, 대표 이미지 지정 버튼 포함
import { NodeViewWrapper } from '@tiptap/react';
import styles from './RichTextEditor.module.scss';

export default function CoverImageView({ node, editor, getPos }) {
  const isCover = node.attrs.isCover;

  // 대표 이미지는 항상 하나만 유지, 새로 지정하기 전에 기존 표시를 먼저 해제
  function handleToggleCover(e) {
    e.preventDefault();
    e.stopPropagation();
    if (isCover || typeof getPos !== 'function') return;
    const targetPos = getPos();
    const tr = editor.state.tr;
    editor.state.doc.descendants((n, pos) => {
      if (n.type.name === 'image' && n.attrs.isCover) {
        tr.setNodeAttribute(pos, 'isCover', false);
      }
    });
    tr.setNodeAttribute(targetPos, 'isCover', true);
    editor.view.dispatch(tr);
  }

  return (
    <NodeViewWrapper as="span" className={styles.editorImageWrapper} draggable="true" data-drag-handle>
      <img src={node.attrs.src} alt={node.attrs.alt ?? ''} className={styles.editorImage} />
      <button
        type="button"
        className={`${styles.coverToggle} ${isCover ? styles.coverToggleActive : ''}`}
        onMouseDown={(e) => e.preventDefault()}
        onClick={handleToggleCover}
      >
        {isCover ? '✓ 대표' : '대표로 설정'}
      </button>
    </NodeViewWrapper>
  );
}

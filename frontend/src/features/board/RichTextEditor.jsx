// Tiptap 기반 리치 텍스트 에디터, 서식과 이미지 삽입 툴바 포함
import { useRef } from 'react';
import { EditorContent, useEditor } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import Underline from '@tiptap/extension-underline';
import { TextStyle, FontSize } from '@tiptap/extension-text-style';
import TextAlign from '@tiptap/extension-text-align';
import Placeholder from '@tiptap/extension-placeholder';
import { CoverImage } from './CoverImageExtension';
import { PhotoIcon } from '../../components/Icon/Icon';
import styles from './RichTextEditor.module.scss';

const FONT_SIZES = [
  { label: '작게', value: '13px' },
  { label: '보통', value: '15px' },
  { label: '크게', value: '20px' },
  { label: '아주 크게', value: '28px' },
];

export default function RichTextEditor({ value, onChange, onImageFileSelected, placeholder }) {
  const fileInputRef = useRef(null);

  const editor = useEditor({
    extensions: [
      StarterKit,
      Underline,
      TextStyle,
      FontSize,
      TextAlign.configure({ types: ['heading', 'paragraph'] }),
      CoverImage,
      Placeholder.configure({ placeholder }),
    ],
    content: value,
    onUpdate: ({ editor: current }) => {
      onChange(current.getHTML());
    },
  });

  function handleImageButtonClick() {
    fileInputRef.current?.click();
  }

  // 선택한 파일을 임시 blob URL로 미리보기 삽입, 실제 업로드는 저장 시점에 처리
  function handleFileChange(e) {
    const file = e.target.files[0];
    e.target.value = '';
    if (!file || !editor) return;
    const blobUrl = URL.createObjectURL(file);
    // 첫 번째로 삽입되는 이미지를 대표 이미지로 지정
    let hasExistingImage = false;
    editor.state.doc.descendants((node) => {
      if (node.type.name === 'image') hasExistingImage = true;
    });
    const pos = editor.state.selection.to;
    editor
      .chain()
      .focus()
      .insertContentAt(pos, { type: 'image', attrs: { src: blobUrl, isCover: !hasExistingImage } })
      .setTextSelection(pos + 1)
      .run();
    onImageFileSelected(file, blobUrl);
  }

  if (!editor) {
    return null;
  }

  return (
    <div className={styles.wrapper}>
      <div className={styles.toolbar}>
        <button
          type="button"
          className={editor.isActive('bold') ? styles.active : ''}
          onMouseDown={(e) => e.preventDefault()}
          onClick={() => editor.chain().focus().toggleBold().run()}
          title="굵게"
        >
          <b>B</b>
        </button>
        <button
          type="button"
          className={editor.isActive('italic') ? styles.active : ''}
          onMouseDown={(e) => e.preventDefault()}
          onClick={() => editor.chain().focus().toggleItalic().run()}
          title="기울임"
        >
          <i>I</i>
        </button>
        <button
          type="button"
          className={editor.isActive('underline') ? styles.active : ''}
          onMouseDown={(e) => e.preventDefault()}
          onClick={() => editor.chain().focus().toggleUnderline().run()}
          title="밑줄"
        >
          <u>U</u>
        </button>

        <span className={styles.divider} />

        <select
          className={styles.fontSizeSelect}
          defaultValue=""
          onChange={(e) => {
            if (e.target.value) {
              editor.chain().focus().setFontSize(e.target.value).run();
            }
            e.target.value = '';
          }}
        >
          <option value="" disabled>
            글자 크기
          </option>
          {FONT_SIZES.map((size) => (
            <option key={size.value} value={size.value}>
              {size.label}
            </option>
          ))}
        </select>

        <span className={styles.divider} />

        <button
          type="button"
          className={editor.isActive({ textAlign: 'left' }) ? styles.active : ''}
          onMouseDown={(e) => e.preventDefault()}
          onClick={() => editor.chain().focus().setTextAlign('left').run()}
          title="왼쪽 정렬"
        >
          ⇤
        </button>
        <button
          type="button"
          className={editor.isActive({ textAlign: 'center' }) ? styles.active : ''}
          onMouseDown={(e) => e.preventDefault()}
          onClick={() => editor.chain().focus().setTextAlign('center').run()}
          title="가운데 정렬"
        >
          ⇔
        </button>
        <button
          type="button"
          className={editor.isActive({ textAlign: 'right' }) ? styles.active : ''}
          onMouseDown={(e) => e.preventDefault()}
          onClick={() => editor.chain().focus().setTextAlign('right').run()}
          title="오른쪽 정렬"
        >
          ⇥
        </button>

        <span className={styles.divider} />

        <button type="button" onMouseDown={(e) => e.preventDefault()} onClick={handleImageButtonClick} title="사진 삽입">
          <PhotoIcon /> 사진
        </button>
        <input
          ref={fileInputRef}
          type="file"
          accept="image/jpeg,image/png,image/gif,image/webp"
          hidden
          onChange={handleFileChange}
        />
      </div>
      <EditorContent editor={editor} className={styles.content} />
    </div>
  );
}

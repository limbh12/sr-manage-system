import React, { useRef, useEffect } from 'react';
import { Editor } from '@toast-ui/react-editor';
import '@toast-ui/editor/dist/toastui-editor.css';
import { wikiFileApi } from '../../services/wikiService';

interface WikiEditorProps {
  initialValue?: string;
  onChange?: (content: string) => void;
  height?: string;
  documentId?: number;
}

const WikiEditor: React.FC<WikiEditorProps> = ({
  initialValue = '',
  onChange,
  height = '600px',
  documentId,
}) => {
  const editorRef = useRef<Editor>(null);
  const isInitializedRef = useRef(false);

  // 초기 값 설정 (최초 1회만)
  useEffect(() => {
    if (editorRef.current && !isInitializedRef.current) {
      const editorInstance = editorRef.current.getInstance();
      if (initialValue) {
        editorInstance.setMarkdown(initialValue);
      }
      isInitializedRef.current = true;
    }
  }, [initialValue]);

  const handleChange = () => {
    if (editorRef.current && onChange) {
      const editorInstance = editorRef.current.getInstance();
      const content = editorInstance.getMarkdown();
      onChange(content);
    }
  };

  const handleImageUpload = async (blob: File, callback: (url: string, altText: string) => void) => {
    try {
      const response = await wikiFileApi.upload(blob, documentId);
      const imageUrl = wikiFileApi.getDownloadUrl(response.data.id);
      callback(imageUrl, blob.name);
    } catch (error) {
      console.error('이미지 업로드 실패:', error);
      alert('이미지 업로드에 실패했습니다.');
    }
  };

  return (
    <div className="wiki-editor-container">
      <Editor
        ref={editorRef}
        initialValue={initialValue}
        height={height}
        initialEditType="markdown"
        useCommandShortcut={true}
        hideModeSwitch={false}
        onChange={handleChange}
        hooks={{
          addImageBlobHook: handleImageUpload,
        }}
        toolbarItems={[
          ['heading', 'bold', 'italic', 'strike'],
          ['hr', 'quote'],
          ['ul', 'ol', 'task', 'indent', 'outdent'],
          ['table', 'image', 'link'],
          ['code', 'codeblock'],
        ]}
      />
    </div>
  );
};

export default WikiEditor;

/**
 * 전화번호 포맷팅 (010-1234-5678)
 * @param value 입력된 전화번호 문자열
 * @returns 하이픈이 포함된 포맷팅된 문자열
 */
export const formatPhoneNumber = (value: string | undefined | null): string => {
  if (!value) return '';
  
  // 숫자만 추출
  const clean = value.replace(/[^0-9]/g, '');
  
  // 길이에 따른 포맷팅
  if (clean.length < 4) {
    return clean;
  }
  
  // 서울 지역번호(02)인 경우
  if (clean.startsWith('02')) {
    if (clean.length < 7) {
      return `${clean.slice(0, 2)}-${clean.slice(2)}`;
    }
    if (clean.length < 10) {
      return `${clean.slice(0, 2)}-${clean.slice(2, 5)}-${clean.slice(5)}`;
    }
    return `${clean.slice(0, 2)}-${clean.slice(2, 6)}-${clean.slice(6, 10)}`;
  }
  
  // 그 외 (010, 031 등)
  if (clean.length < 7) {
    return `${clean.slice(0, 3)}-${clean.slice(3)}`;
  }
  if (clean.length < 11) {
    return `${clean.slice(0, 3)}-${clean.slice(3, 6)}-${clean.slice(6)}`;
  }
  return `${clean.slice(0, 3)}-${clean.slice(3, 7)}-${clean.slice(7, 11)}`;
};

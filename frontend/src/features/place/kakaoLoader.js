// 카카오맵 SDK 스크립트를 한 번만 로드하는 로더
let loaderPromise = null;

export function loadKakaoMaps() {
  if (window.kakao?.maps) {
    return Promise.resolve(window.kakao);
  }
  if (loaderPromise) {
    return loaderPromise;
  }

  loaderPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script');
    script.src = `//dapi.kakao.com/v2/maps/sdk.js?appkey=${import.meta.env.VITE_KAKAO_MAP_KEY}&autoload=false&libraries=clusterer`;
    script.onload = () => {
      window.kakao.maps.load(() => resolve(window.kakao));
    };
    script.onerror = () => {
      loaderPromise = null;
      reject(new Error('카카오맵 SDK를 불러오지 못했습니다.'));
    };
    document.head.appendChild(script);
  });

  return loaderPromise;
}

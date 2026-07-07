"use client";

import * as React from "react";

export interface TypewriterOptions {
  /** 每个字符的间隔(ms),默认 18 */
  speed?: number;
  /** 是否启用打字机效果,false 时直接全部显示 */
  enabled?: boolean;
  /** 打字结束回调 */
  onDone?: () => void;
}

export function useTypewriter(
  fullText: string,
  options: TypewriterOptions = {}
) {
  const { speed = 18, enabled = true, onDone } = options;
  const [displayed, setDisplayed] = React.useState(enabled ? "" : fullText);
  const [done, setDone] = React.useState(!enabled);
  const indexRef = React.useRef(0);
  const timerRef = React.useRef<ReturnType<typeof setTimeout> | null>(null);
  const doneRef = React.useRef(onDone);
  doneRef.current = onDone;

  React.useEffect(() => {
    if (!enabled) {
      setDisplayed(fullText);
      setDone(true);
      return;
    }
    indexRef.current = 0;
    setDisplayed("");
    setDone(false);

    const tick = () => {
      const i = indexRef.current;
      if (i >= fullText.length) {
        setDone(true);
        doneRef.current?.();
        return;
      }
      indexRef.current = i + 1;
      setDisplayed(fullText.slice(0, i + 1));
      if (i + 1 < fullText.length) {
        timerRef.current = setTimeout(tick, speed);
      } else {
        setDone(true);
        doneRef.current?.();
      }
    };

    if (fullText.length > 0) {
      timerRef.current = setTimeout(tick, speed);
    } else {
      setDone(true);
      doneRef.current?.();
    }

    return () => {
      if (timerRef.current) {
        clearTimeout(timerRef.current);
        timerRef.current = null;
      }
    };
  }, [fullText, enabled, speed]);

  const skip = React.useCallback(() => {
    if (timerRef.current) {
      clearTimeout(timerRef.current);
      timerRef.current = null;
    }
    indexRef.current = fullText.length;
    setDisplayed(fullText);
    setDone(true);
    doneRef.current?.();
  }, [fullText]);

  return { displayed, done, skip };
}

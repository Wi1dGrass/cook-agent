import type { ErrorResponse } from "./types";

export class ApiError extends Error {
  code: string;
  status: number;
  constructor(code: string, message: string, status: number) {
    super(message);
    this.name = "ApiError";
    this.code = code;
    this.status = status;
  }
}

export const ERROR_CODE_LABELS: Record<string, string> = {
  PARAM_INVALID: "参数无效",
  VALIDATION_ERROR: "数据校验失败",
  NOT_FOUND: "资源不存在",
  RATE_LIMITED: "请求过于频繁，请稍后再试",
  UNAUTHORIZED: "未登录或登录已过期",
  FORBIDDEN: "无权限执行该操作",
  SEARCH_NO_RESULTS: "未找到匹配结果",
  AGENT_ERROR: "Agent 执行出错",
  INTERNAL_ERROR: "服务器内部错误",
};

export function friendlyMessage(err: ErrorResponse | ApiError | unknown): string {
  if (err instanceof ApiError) {
    return ERROR_CODE_LABELS[err.code] ?? err.message ?? "请求失败";
  }
  const e = err as ErrorResponse;
  if (e && typeof e.code === "string") {
    return ERROR_CODE_LABELS[e.code] ?? e.message ?? "请求失败";
  }
  return "网络异常，请稍后重试";
}

export async function parseError(res: Response): Promise<ApiError> {
  let body: ErrorResponse | null = null;
  try {
    body = (await res.json()) as ErrorResponse;
  } catch {
    body = null;
  }
  const code = body?.code ?? "HTTP_" + res.status;
  const message = body?.message ?? res.statusText ?? "请求失败";
  return new ApiError(code, message, res.status);
}

export async function parseErrorClient(res: Response): Promise<ApiError> {
  return parseError(res);
}

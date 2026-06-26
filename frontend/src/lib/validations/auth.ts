import { z } from "zod";

export const loginSchema = z.object({
  username: z.string().min(1, "请输入用户名").max(32, "用户名过长"),
  password: z.string().min(1, "请输入密码").max(64, "密码过长"),
});

export const registerSchema = z.object({
  username: z
    .string()
    .min(3, "用户名至少 3 个字符")
    .max(32, "用户名最多 32 个字符"),
  password: z
    .string()
    .min(6, "密码至少 6 个字符")
    .max(64, "密码最多 64 个字符"),
  phone: z
    .string()
    .optional()
    .refine((v) => !v || /^1\d{10}$/.test(v), "手机号格式不正确（11 位，以 1 开头）"),
});

export type LoginValues = z.infer<typeof loginSchema>;
export type RegisterValues = z.infer<typeof registerSchema>;

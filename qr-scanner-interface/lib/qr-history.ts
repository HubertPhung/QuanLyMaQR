import { Globe, Wifi, Contact, CreditCard, type LucideIcon } from "lucide-react"

export type ScanType = "url" | "wifi" | "contact" | "payment"

export type ScanItem = {
  id: string
  type: ScanType
  title: string
  subtitle: string
  time: string
}

export const typeMeta: Record<ScanType, { icon: LucideIcon; label: string }> = {
  url: { icon: Globe, label: "Liên kết" },
  wifi: { icon: Wifi, label: "Wi-Fi" },
  contact: { icon: Contact, label: "Danh bạ" },
  payment: { icon: CreditCard, label: "Thanh toán" },
}

export const initialHistory: ScanItem[] = [
  { id: "1", type: "url", title: "vercel.com/dashboard", subtitle: "Liên kết website", time: "Vừa xong" },
  { id: "2", type: "wifi", title: "Coffee_House_5G", subtitle: "Mạng Wi-Fi", time: "2 phút trước" },
  { id: "3", type: "payment", title: "240.000đ", subtitle: "Chuyển khoản QR", time: "18 phút trước" },
  { id: "4", type: "contact", title: "Nguyễn Minh Anh", subtitle: "Danh thiếp liên hệ", time: "Hôm qua" },
  { id: "5", type: "url", title: "github.com/vercel/next.js", subtitle: "Kho mã nguồn", time: "Hôm qua" },
  { id: "6", type: "wifi", title: "Home_Network_2.4G", subtitle: "Mạng Wi-Fi", time: "2 ngày trước" },
  { id: "7", type: "payment", title: "89.000đ", subtitle: "Thanh toán quán ăn", time: "3 ngày trước" },
]

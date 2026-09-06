"use client"

import { Home, ScanLine, QrCode, Clock } from "lucide-react"

export type Screen = "home" | "scan" | "create" | "history"

const items: { id: Screen; label: string; icon: typeof Home }[] = [
  { id: "home", label: "Trang chủ", icon: Home },
  { id: "scan", label: "Quét mã", icon: ScanLine },
  { id: "create", label: "Tạo mã", icon: QrCode },
  { id: "history", label: "Lịch sử", icon: Clock },
]

export function BottomNav({
  active,
  onChange,
}: {
  active: Screen
  onChange: (screen: Screen) => void
}) {
  return (
    <nav
      aria-label="Điều hướng chính"
      className="sticky bottom-0 z-10 mt-auto border-t border-border bg-background/80 px-2 py-2 backdrop-blur-lg"
    >
      <ul className="flex items-center justify-around">
        {items.map((item) => {
          const Icon = item.icon
          const isActive = active === item.id
          return (
            <li key={item.id}>
              <button
                type="button"
                onClick={() => onChange(item.id)}
                aria-current={isActive ? "page" : undefined}
                className={`flex flex-col items-center gap-1 rounded-xl px-4 py-1.5 transition-colors ${
                  isActive ? "text-scan" : "text-muted-foreground hover:text-foreground"
                }`}
              >
                <Icon className="h-5 w-5" />
                <span className="text-[0.65rem] font-medium">{item.label}</span>
              </button>
            </li>
          )
        })}
      </ul>
    </nav>
  )
}

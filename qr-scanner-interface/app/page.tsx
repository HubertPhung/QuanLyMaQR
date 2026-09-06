"use client"

import { useState } from "react"
import { Settings2 } from "lucide-react"
import { BottomNav, type Screen } from "@/components/bottom-nav"
import { HomeScreen } from "@/components/screens/home-screen"
import { ScanScreen } from "@/components/screens/scan-screen"
import { CreateScreen } from "@/components/screens/create-screen"
import { HistoryScreen } from "@/components/screens/history-screen"
import { initialHistory, type ScanItem } from "@/lib/qr-history"

const titles: Record<Screen, { title: string; subtitle: string }> = {
  home: { title: "QR Scanner", subtitle: "Quét & tạo mã nhanh chóng" },
  scan: { title: "Quét mã QR", subtitle: "Đưa mã vào khung để quét" },
  create: { title: "Tạo mã QR", subtitle: "Tạo mã của riêng bạn" },
  history: { title: "Lịch sử", subtitle: "Các mã đã quét gần đây" },
}

export default function Page() {
  const [screen, setScreen] = useState<Screen>("home")
  const [history, setHistory] = useState<ScanItem[]>(initialHistory)

  function deleteItem(id: string) {
    setHistory((prev) => prev.filter((item) => item.id !== id))
  }

  function clearAll() {
    setHistory([])
  }

  const header = titles[screen]

  return (
    <main className="flex min-h-dvh justify-center bg-background">
      <div className="flex w-full max-w-md flex-col">
        {/* Header */}
        <header className="flex items-center justify-between px-5 pt-6">
          <div>
            <h1 className="text-lg font-semibold leading-tight text-foreground">{header.title}</h1>
            <p className="text-xs text-muted-foreground">{header.subtitle}</p>
          </div>
          <button
            type="button"
            aria-label="Cài đặt"
            className="flex h-10 w-10 items-center justify-center rounded-xl border border-border bg-card text-muted-foreground transition-colors hover:text-foreground"
          >
            <Settings2 className="h-5 w-5" />
          </button>
        </header>

        {/* Screen content */}
        <div className="flex-1 px-5 pb-4">
          {screen === "home" && (
            <HomeScreen
              history={history}
              onScan={() => setScreen("scan")}
              onCreate={() => setScreen("create")}
              onViewAll={() => setScreen("history")}
            />
          )}
          {screen === "scan" && (
            <ScanScreen history={history} onViewAll={() => setScreen("history")} />
          )}
          {screen === "create" && <CreateScreen />}
          {screen === "history" && (
            <HistoryScreen
              history={history}
              onBack={() => setScreen("scan")}
              onDelete={deleteItem}
              onClearAll={clearAll}
            />
          )}
        </div>

        <BottomNav active={screen} onChange={setScreen} />
      </div>
    </main>
  )
}

"use client"

import { ImageIcon } from "lucide-react"
import { QrScannerFrame } from "@/components/qr-scanner-frame"
import { ScanHistory } from "@/components/scan-history"
import type { ScanItem } from "@/lib/qr-history"

export function ScanScreen({
  history,
  onViewAll,
}: {
  history: ScanItem[]
  onViewAll: () => void
}) {
  return (
    <div className="flex flex-col">
      <div className="flex flex-col items-center py-10">
        <QrScannerFrame />
        <p className="mt-10 max-w-[16rem] text-balance text-center text-sm text-muted-foreground">
          Giữ điện thoại ổn định và căn mã QR nằm gọn trong khung viền.
        </p>
      </div>

      <div className="mb-8 flex justify-center gap-3">
        <button
          type="button"
          className="flex items-center gap-2 rounded-full border border-border bg-card px-5 py-2.5 text-sm font-medium text-foreground transition-colors hover:bg-secondary"
        >
          <ImageIcon className="h-4 w-4 text-scan" />
          Chọn từ thư viện
        </button>
      </div>

      <ScanHistory items={history} onViewAll={onViewAll} />
    </div>
  )
}

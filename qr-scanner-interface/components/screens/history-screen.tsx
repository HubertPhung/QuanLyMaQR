"use client"

import { Trash2, ChevronLeft, QrCode } from "lucide-react"
import { typeMeta, type ScanItem } from "@/lib/qr-history"

export function HistoryScreen({
  history,
  onBack,
  onDelete,
  onClearAll,
}: {
  history: ScanItem[]
  onBack: () => void
  onDelete: (id: string) => void
  onClearAll: () => void
}) {
  return (
    <div className="flex flex-col py-2">
      {/* Sub-header */}
      <div className="mb-5 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={onBack}
            aria-label="Quay lại"
            className="flex h-9 w-9 items-center justify-center rounded-xl border border-border bg-card text-muted-foreground transition-colors hover:text-foreground"
          >
            <ChevronLeft className="h-5 w-5" />
          </button>
          <div>
            <h2 className="text-base font-semibold text-foreground">Lịch sử quét</h2>
            <p className="text-xs text-muted-foreground">{history.length} mã đã quét</p>
          </div>
        </div>
        {history.length > 0 && (
          <button
            type="button"
            onClick={onClearAll}
            className="flex items-center gap-1.5 rounded-full border border-destructive/30 bg-destructive/10 px-3 py-2 text-xs font-medium text-destructive transition-colors hover:bg-destructive/20"
          >
            <Trash2 className="h-3.5 w-3.5" />
            Xóa tất cả
          </button>
        )}
      </div>

      {/* List */}
      {history.length === 0 ? (
        <div className="flex flex-col items-center gap-3 rounded-3xl border border-border bg-card px-6 py-16 text-center">
          <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-secondary text-muted-foreground">
            <QrCode className="h-7 w-7" />
          </div>
          <div>
            <p className="text-sm font-medium text-foreground">Chưa có lịch sử</p>
            <p className="mt-1 text-xs text-muted-foreground">
              Các mã QR bạn quét sẽ xuất hiện tại đây.
            </p>
          </div>
        </div>
      ) : (
        <ul className="flex flex-col gap-2.5">
          {history.map((item) => {
            const { icon: Icon, label } = typeMeta[item.type]
            return (
              <li key={item.id}>
                <div className="group flex items-center gap-3 rounded-2xl border border-border bg-card p-3">
                  <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-secondary text-scan">
                    <Icon className="h-5 w-5" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium text-foreground">{item.title}</p>
                    <p className="truncate text-xs text-muted-foreground">
                      {label} · {item.time}
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={() => onDelete(item.id)}
                    aria-label={`Xóa ${item.title}`}
                    className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl border border-border text-muted-foreground transition-colors hover:border-destructive/40 hover:bg-destructive/10 hover:text-destructive"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
              </li>
            )
          })}
        </ul>
      )}
    </div>
  )
}

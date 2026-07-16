"use client";

import { useState } from "react";

interface Props {
  selected: Date;
  onSelect: (d: Date) => void;
}

function isoDate(d: Date): string {
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
}

const DOW = ["M", "T", "W", "T", "F", "S", "S"];

/** Small month-at-a-glance calendar for jumping the scheduler to a date. */
export default function MiniCalendar({ selected, onSelect }: Props) {
  const [month, setMonth] = useState(() => new Date(selected.getFullYear(), selected.getMonth(), 1));

  const todayIso = isoDate(new Date());
  const selIso = isoDate(selected);

  // Grid starts on the Monday on/before the 1st.
  const first = new Date(month.getFullYear(), month.getMonth(), 1);
  const offset = (first.getDay() + 6) % 7;
  const gridStart = new Date(first);
  gridStart.setDate(first.getDate() - offset);

  const cells: Date[] = [];
  for (let i = 0; i < 42; i++) {
    const d = new Date(gridStart);
    d.setDate(gridStart.getDate() + i);
    cells.push(d);
  }

  const shiftMonth = (delta: number) =>
    setMonth(new Date(month.getFullYear(), month.getMonth() + delta, 1));

  return (
    <div className="mini-cal">
      <div className="mini-head">
        <button className="icon-btn" onClick={() => shiftMonth(-1)} aria-label="Previous month">
          ‹
        </button>
        <span>{month.toLocaleDateString("en-US", { month: "long", year: "numeric" })}</span>
        <button className="icon-btn" onClick={() => shiftMonth(1)} aria-label="Next month">
          ›
        </button>
      </div>
      <div className="mini-grid">
        {DOW.map((d, i) => (
          <span key={i} className="dow">
            {d}
          </span>
        ))}
        {cells.map((d) => {
          const iso = isoDate(d);
          const dim = d.getMonth() !== month.getMonth();
          const cls = [
            "mini-day",
            dim ? "dim" : "",
            iso === todayIso ? "today" : "",
            iso === selIso ? "sel" : "",
          ]
            .filter(Boolean)
            .join(" ");
          return (
            <button key={iso} className={cls} onClick={() => onSelect(new Date(d))}>
              {d.getDate()}
            </button>
          );
        })}
      </div>
    </div>
  );
}

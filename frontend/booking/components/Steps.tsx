const LABELS = ["Add-ons", "Time", "Details", "Verify"];

/** Booking progress dots (1-based current step). */
export default function Steps({ current }: { current: number }) {
  return (
    <div className="steps" aria-label={`Step ${current} of ${LABELS.length}: ${LABELS[current - 1]}`}>
      {LABELS.map((label, i) => {
        const step = i + 1;
        const cls = step === current ? "dot current" : step < current ? "dot done" : "dot";
        return (
          <span key={label} style={{ display: "inline-flex", alignItems: "center", gap: "0.4rem" }}>
            {i > 0 && <span className={step <= current ? "bar done" : "bar"} />}
            <span className={cls} title={label}>
              {step < current ? "✓" : step}
            </span>
          </span>
        );
      })}
    </div>
  );
}

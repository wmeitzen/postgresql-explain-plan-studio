# Search and warnings

Find operators on the diagram and tune when warning chips appear.

## Find on the diagram

1. Press **Ctrl+F** (Windows/Linux) or **Cmd+F** (macOS), or enable **Find**.
2. Type a query; matches update live.
3. Optionally filter by warning kinds: **Time**, **Cost**, **Rows**, **Filter**, **Heap**.
4. Use **Next** / **Previous** (or **Enter** / **F3**) to step through matches.
5. **Esc** closes the find bar.

When nothing matches, the query field uses a high-contrast no-match style so it stays readable.

**Screenshot:** Find bar with filters

![Find bar](images/ss-find-bar.png)

<!-- Drop file at docs/user/images/ss-find-bar.png -->

## Warning indicators

Indicators appear north of some operators. Hover over an indicator to see its value.

- **Actual vs Estimate ratio** (balance scale) — operators where estimated and actual values differ by orders of magnitude.
- **Cost %** (dollar sign) — the operators with the highest individual cost. Usually only the top two are marked; more appear only when their percentages are equal.
- **Filter removes rows** (funnel) — operators that discard a significant amount of data (mass or rows) before emitting the rest.
- **Heap fetch** — index-only scans that still had to visit the table heap for a high share of rows, which usually means the visibility map is out of date. Consider running VACUUM on that table.
- **Opr time %** (analog clock) — the operators that took the longest. Usually only the top two are marked; more appear only when their percentages are equal.

**Screenshot:** Warning indicators on a diagram

![Warning indicators](images/ss-warning-indicators.png)

<!-- Drop file at docs/user/images/ss-warning-indicators.png -->

## Warning thresholds

In application **Settings**, adjust thresholds that drive chips and Find filters, including:

- Minimum actual rows / mass
- Actual vs estimated factor
- Filter %
- Heap fetch %

**Screenshot:** Warning threshold Settings

![Warning settings](images/ss-warning-settings.png)

<!-- Drop file at docs/user/images/ss-warning-settings.png -->

## Next

- [Metrics and compare](06-metrics-and-compare.md)

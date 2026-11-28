# Search and warnings

Find operators on the diagram and tune when warning chips appear.

## Find on the diagram

1. Press **Ctrl+F** (Windows/Linux) or **Cmd+F** (macOS), or enable **Find**.
2. Optionally filter by warning kinds: **Time**, **Cost**, **Rows**, **Filter**, **Heap**.
3. Use **Next** / **Previous** (or **Enter** / **F3**) to step through matches.
4. **Esc** closes the find bar.

**Screenshot:** Find bar with filters

![Find bar](images/ss-find-bar.png)

<!-- Drop file at docs/user/images/ss-find-bar.png -->

## Warning indicators

Indicators appear north of some operators. Hover over an indicator to see its value.

<!--
  Avoid a side-by-side table here: Markdown preview / GitHub CSS often force
  vertical-align on cells (and strip style/valign), which parks the icon+label
  at the bottom of tall Meaning rows. Stacked entries keep icon, name, and
  text top-aligned without fighting that.
-->

<p>
<img src="images/ss-warning-icon-actual-vs-estimate.png" alt="Actual vs Estimate ratio icon" width="28" height="28">
<strong>Actual vs Estimate ratio</strong><br>
Operators where estimated and actual values differ by orders of magnitude.
</p>

<p>
<img src="images/ss-warning-icon-cost.png" alt="Cost % icon" width="28" height="28">
<strong>Cost %</strong><br>
The operators with the highest individual cost. Usually only the top two are marked; more appear only when their percentages are equal.
</p>

<p>
<img src="images/ss-warning-icon-filter.png" alt="Filter removes rows icon" width="28" height="28">
<strong>Filter removes rows</strong><br>
Operators that discard a significant amount of data before emitting the rest.
</p>

<p>
<img src="images/ss-warning-icon-heap.png" alt="Heap fetch icon" width="28" height="28">
<strong>Heap fetch</strong><br>
Index-only scans that still had to visit the table heap for a high share of rows, which usually means the visibility map is out of date. Consider running VACUUM on that table.
</p>

<p>
<img src="images/ss-warning-icon-time.png" alt="Opr time % icon" width="28" height="28">
<strong>Opr time %</strong><br>
The operators that took the longest. Usually only the top two are marked; more appear only when their percentages are equal.
</p>

Example of where warning indicators will appear.

![Warning indicators above an operator](images/ss-warning-indicators.png)

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

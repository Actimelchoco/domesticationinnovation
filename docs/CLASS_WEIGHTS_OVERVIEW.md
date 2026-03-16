# Class Weights Overview

`class_weights.json` is still the runtime source of truth.

If you want an easier table view in Excel, regenerate the CSV overview files with:

```powershell
python scripts/export_class_weights_overview.py
```

After editing the CSVs in Excel, write the changes back into `class_weights.json` with:

```powershell
python scripts/export_class_weights_overview.py import
```

The importer accepts normal CSV output and also the common Excel variants with semicolon separators and comma decimals.

If you want a single formatted Excel workbook built from the CSVs, run:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/build_class_weights_workbook.ps1
```

To build it and leave it open in Excel:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/build_class_weights_workbook.ps1 -Open
```

If you want the normal editor workflow, use the shortcut that first refreshes all CSVs from `class_weights.json` and then opens the workbook:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/openClassEditor.ps1
```

or from `cmd` / PowerShell:

```powershell
.\openClassEditor.cmd
```

That creates:

- `docs/class_weights_overview/class_weights_overview.xlsm`

The workbook includes:

- the 5 source sheets
- a `class_view` sheet with a class dropdown
- a `Reload Class` button
- an `Apply Edits To JSON` button

The edit button needs Excel macros enabled. If Excel blocks the macro/button wiring entirely, enable:

- `Trust access to the VBA project object model`

This writes spreadsheet-friendly matrices to:

- `docs/class_weights_overview/meta.csv`
- `docs/class_weights_overview/categories.csv`
- `docs/class_weights_overview/base_stats.csv`
- `docs/class_weights_overview/attributes.csv`
- `docs/class_weights_overview/abilities.csv`

Each CSV can be opened directly in Excel.

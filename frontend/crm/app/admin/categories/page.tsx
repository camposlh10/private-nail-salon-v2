"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import {
  AdminCategory,
  ApiError,
  changeCategoryStatus,
  createCategory,
  listCategories,
  reorderCategories,
  updateCategory,
} from "@/lib/api";

export default function CategoriesPage() {
  const [categories, setCategories] = useState<AdminCategory[]>([]);
  const [newName, setNewName] = useState("");
  const [editing, setEditing] = useState<AdminCategory | null>(null);
  const [editName, setEditName] = useState("");
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(async () => {
    try {
      setCategories(await listCategories());
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load categories");
    }
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  const run = async (action: () => Promise<unknown>) => {
    setError(null);
    try {
      await action();
      await reload();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Something went wrong");
    }
  };

  const handleCreate = (event: FormEvent) => {
    event.preventDefault();
    if (!newName.trim()) return;
    run(async () => {
      await createCategory(newName.trim());
      setNewName("");
    });
  };

  const handleRename = (event: FormEvent) => {
    event.preventDefault();
    if (!editing) return;
    run(async () => {
      await updateCategory(editing.id, editName.trim(), editing.description, editing.version);
      setEditing(null);
    });
  };

  const move = (index: number, delta: number) => {
    const target = index + delta;
    if (target < 0 || target >= categories.length) return;
    const ids = categories.map((c) => c.id);
    [ids[index], ids[target]] = [ids[target], ids[index]];
    run(() => reorderCategories(ids));
  };

  return (
    <>
      <h1>Categories</h1>
      {error && <div className="error">{error}</div>}

      <form className="card row" onSubmit={handleCreate} style={{ alignItems: "flex-end" }}>
        <div>
          <label htmlFor="new-category">New category name</label>
          <input
            id="new-category"
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            placeholder="e.g. Manicure"
          />
        </div>
        <div style={{ flex: "0 0 auto" }}>
          <button type="submit">Add category</button>
        </div>
      </form>

      <table>
        <thead>
          <tr>
            <th>Order</th>
            <th>Name</th>
            <th>Slug</th>
            <th>Status</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {categories.map((category, index) => (
            <tr key={category.id}>
              <td>
                <button className="secondary small" onClick={() => move(index, -1)} title="Move up">
                  ↑
                </button>{" "}
                <button className="secondary small" onClick={() => move(index, 1)} title="Move down">
                  ↓
                </button>
              </td>
              <td>
                {editing?.id === category.id ? (
                  <form onSubmit={handleRename} style={{ display: "flex", gap: "0.4rem" }}>
                    <input value={editName} onChange={(e) => setEditName(e.target.value)} />
                    <button type="submit" className="small">
                      Save
                    </button>
                    <button
                      type="button"
                      className="secondary small"
                      onClick={() => setEditing(null)}
                    >
                      Cancel
                    </button>
                  </form>
                ) : (
                  category.name
                )}
              </td>
              <td className="muted">{category.slug}</td>
              <td>
                <span className={`badge ${category.status}`}>{category.status}</span>
              </td>
              <td style={{ textAlign: "right" }}>
                <button
                  className="secondary small"
                  onClick={() => {
                    setEditing(category);
                    setEditName(category.name);
                  }}
                >
                  Rename
                </button>{" "}
                {category.status === "ARCHIVED" ? (
                  <button
                    className="secondary small"
                    onClick={() => run(() => changeCategoryStatus(category.id, "ACTIVE"))}
                  >
                    Activate
                  </button>
                ) : (
                  <button
                    className="danger small"
                    onClick={() => run(() => changeCategoryStatus(category.id, "ARCHIVED"))}
                  >
                    Archive
                  </button>
                )}
              </td>
            </tr>
          ))}
          {categories.length === 0 && (
            <tr>
              <td colSpan={5} className="muted">
                No categories yet — create your first one above.
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </>
  );
}

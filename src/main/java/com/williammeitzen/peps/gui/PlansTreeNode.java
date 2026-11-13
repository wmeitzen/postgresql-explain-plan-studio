package com.williammeitzen.peps.gui;

/**
 * User object for nodes in the Plans tree (folders and plans).
 */
public final class PlansTreeNode {

	public enum Kind {
		FOLDER,
		PLAN
	}

	private final Kind kind;
	/** Folder id for folder nodes; containing folder id for plan nodes (null = root). */
	private Integer folderId;
	private final Integer planId;
	private String name;

	private PlansTreeNode(Kind kind, Integer folderId, Integer planId, String name) {
		this.kind = kind;
		this.folderId = folderId;
		this.planId = planId;
		this.name = name == null ? "" : name;
	}

	public static PlansTreeNode folder(int folderId, String name) {
		return new PlansTreeNode(Kind.FOLDER, folderId, null, name);
	}

	/** {@code folderId} is the containing folder, or null for root. */
	public static PlansTreeNode plan(int planId, Integer folderId, String name) {
		return new PlansTreeNode(Kind.PLAN, folderId, planId, name);
	}

	public boolean isFolder() {
		return kind == Kind.FOLDER;
	}

	public boolean isPlan() {
		return kind == Kind.PLAN;
	}

	public Kind getKind() {
		return kind;
	}

	/** Folder id for folder nodes; containing folder id for plan nodes (null = root). */
	public Integer getFolderId() {
		return folderId;
	}

	/** Update containing folder for a plan node after Move (folders keep their own id). */
	public void setContainingFolderId(Integer containingFolderId) {
		if (kind == Kind.PLAN) {
			this.folderId = containingFolderId;
		}
	}

	public Integer getPlanId() {
		return planId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name == null ? "" : name;
	}

	@Override
	public String toString() {
		return name;
	}
}

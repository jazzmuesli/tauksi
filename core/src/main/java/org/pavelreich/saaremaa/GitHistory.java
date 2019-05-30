package org.pavelreich.saaremaa;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.Side;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generate git-full-history.csv and git-history.csv without message
 * commit;author;timestamp;changeType;oldFilename;oldSize;newFilename;newSize;message
 * 33ddcb68e17dd1234c4bd0a987bcdaf803761f4a;preich@192.168.1.101;1555541213;MODIFY;build-projects.sh;1062;build-projects.sh;1061;collect
 * pitest
 * 
 * @author preich
 *
 */
public class GitHistory {

	private static final Logger LOG = LoggerFactory.getLogger(GitHistory.class);

	public static void main(String[] args) throws IOException, NoHeadException, GitAPIException {
		Git git = Git.open(new File("."));
		Iterator<RevCommit> it = git.log().call().iterator();
		Repository repository = git.getRepository();

		DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
		df.setRepository(repository);
		df.setDiffComparator(RawTextComparator.DEFAULT);
		df.setDetectRenames(true);

		CSVReporter csvReporter = new CSVReporter("git-history.csv", new String[] { "commit", "author", "timestamp",
				"changeType", "oldFilename", "oldSize", "newFilename", "newSize" });
		CSVReporter fullCsvReporter = new CSVReporter("git-fullm-history.csv", new String[] { "commit", "author",
				"timestamp", "changeType", "oldFilename", "oldSize", "newFilename", "newSize", "message" });
		RevWalk rw = new RevWalk(repository);
		int i = 0;
		while (it.hasNext()) {
			RevCommit commit = it.next();
			RevTree parentTree = getParentTree(rw, commit);
			List<DiffEntry> diffs = df.scan(parentTree, commit.getTree());
			for (DiffEntry diff : diffs) {
				long oldSize = getSize(repository, diff, Side.OLD);
				long newSize = getSize(repository, diff, Side.NEW);
				String fullMessage = commit.getFullMessage().trim().replaceAll("\n", "<CR>");
				csvReporter.write(commit.getId().name(), commit.getAuthorIdent().getEmailAddress(),
						commit.getCommitTime(), diff.getChangeType(), diff.getOldPath(), oldSize, diff.getNewPath(),
						newSize);
				fullCsvReporter.write(commit.getId().name(), commit.getAuthorIdent().getEmailAddress(),
						commit.getCommitTime(), diff.getChangeType(), diff.getOldPath(), oldSize, diff.getNewPath(),
						newSize, fullMessage);

			}
			if (i++ % 10 == 0) { // don't flush too often
				csvReporter.flush();
				fullCsvReporter.flush();
			}
		}
		df.close();
		rw.close();
		csvReporter.close();
		fullCsvReporter.close();
	}

	/**
	 * get parent tree, potentially a null.
	 * 
	 * @param rw
	 * @param commit
	 */
	private static RevTree getParentTree(RevWalk rw, RevCommit commit) {
		if (commit.getParentCount() == 0) {
			return null;
		}
		ObjectId parentId = commit.getParentCount() > 0 ? commit.getParent(0).getId() : ObjectId.zeroId();
		RevTree parentTree;
		try {
			parentTree = commit.getParentCount() > 0 ? rw.parseCommit(parentId).getTree() : null;
			return parentTree;
		} catch (Exception e) {
			LOG.error("Can't get parent tree for " + commit + " on rw " + rw + " due to " + e.getMessage(), e);
			return null;
		}

	}

	private static long getSize(Repository repository, DiffEntry diff, DiffEntry.Side side) {
		AbbreviatedObjectId id = side == Side.OLD ? diff.getOldId() : diff.getNewId();
		String path = side == Side.OLD ? diff.getOldPath() : diff.getNewPath();
		if (path.equals("/dev/null")) {
			return 0;
		}
		ObjectLoader loader;
		try {
			loader = repository.open(id.toObjectId());
			return loader.getSize();
		} catch (Exception e) {
			LOG.error("Can't get size for " + diff + " on side " + side + " due to " + e.getMessage(), e);
			return -1;
		}
	}
}

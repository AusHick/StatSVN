/**
 * Copyright 2005, ObjectLab Financial Ltd
 * All rights protected.
 */

package net.sf.statsvn;

import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;

import net.sf.statcvs.model.Repository;
import net.sf.statcvs.model.Revision;
import net.sf.statcvs.model.SymbolicName;
import net.sf.statcvs.model.VersionedFile;
import net.sf.statsvn.output.SvnConfigurationOptions;

/**
 * Execute a Repository Dump on the TaskLogger.
 * 
 * @author Benoit Xhenseval
 */
public class RepoDump {
	private static final int WIDTH_FOR_NUMBER = 5;

	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private final Repository repository;

	private int totalDelta;

	private int totalLastRev;

	private int totalNumRevision;

	private int totalMisMatch;

	private int numberMisMatch;

	private int totalCurrentLOCPerFile;

	public RepoDump(final Repository repo) {
		repository = repo;
	}

	public void dump() {
		final SortedSet revisions = repository.getRevisions();
		final Set filesViaRevisions = dumpPerRevision(revisions);

		SvnConfigurationOptions.getTaskLogger().info("\n\n#### DUMP PER FILE ####");

		final SortedSet files = repository.getFiles();
		dumpPerFile(files);
		dumpPerTags();

		SvnConfigurationOptions.getTaskLogger().info("----------------------------------");
		SvnConfigurationOptions.getTaskLogger().info("Current Repo Line Code :" + repository.getCurrentLOC());
		SvnConfigurationOptions.getTaskLogger().info("-----Via Files--------------------");
		SvnConfigurationOptions.getTaskLogger().info("Number of Files via Files  :" + files.size());
		SvnConfigurationOptions.getTaskLogger().info(
		        "Sum Current LOC via File   :" + totalCurrentLOCPerFile + "\tDiff with Repo LOC " + (repository.getCurrentLOC() - totalCurrentLOCPerFile) + " "
		                + (repository.getCurrentLOC() - totalCurrentLOCPerFile == 0 ? "OK" : "NOT Ok"));
		SvnConfigurationOptions.getTaskLogger().info("# of File Revision via File:" + totalNumRevision);
		SvnConfigurationOptions.getTaskLogger().info("-----Via Revisions----------------");
		SvnConfigurationOptions.getTaskLogger().info(
		        "# of Files via Revisions   :" + padIntRight(filesViaRevisions.size(), WIDTH_FOR_NUMBER) + "\tDiff with via Files:"
		                + (files.size() - filesViaRevisions.size()) + (files.size() - filesViaRevisions.size() == 0 ? " OK" : "NOT Ok"));
		SvnConfigurationOptions.getTaskLogger().info(
		        "# of File Revision via Revi:" + padIntRight(revisions.size(), WIDTH_FOR_NUMBER) + "\tDiff with via Files:"
		                + (totalNumRevision - revisions.size()) + (totalNumRevision - revisions.size() == 0 ? " OK" : "NOT Ok"));
		SvnConfigurationOptions.getTaskLogger().info(
		        "Sum Delta via Revisions    :" + padIntRight(totalDelta, WIDTH_FOR_NUMBER) + "\tDiff with Repo LOC "
		                + (repository.getCurrentLOC() - totalDelta) + " " + (repository.getCurrentLOC() - totalDelta == 0 ? "OK" : "NOT Ok"));
		if (numberMisMatch > 0) {
			SvnConfigurationOptions.getTaskLogger().info("**** PROBLEM ******");
			SvnConfigurationOptions.getTaskLogger().info(
			        "Number of Mismatches       :" + padIntRight(numberMisMatch, WIDTH_FOR_NUMBER) + "\tLOC Mismatch:" + totalMisMatch);
		}
		SvnConfigurationOptions.getTaskLogger().info(
		        "Tot LOC via Last Rev file  :" + padIntRight(totalLastRev, WIDTH_FOR_NUMBER) + "\tDiff with Repo LOC "
		                + (repository.getCurrentLOC() - totalLastRev) + (repository.getCurrentLOC() - totalDelta == 0 ? " OK" : " NOT Ok"));
		SvnConfigurationOptions.getTaskLogger().info("----------------------------------");
	}

	private void dumpPerTags() {
		if (repository.getSymbolicNames() != null) {
			SvnConfigurationOptions.getTaskLogger().info("\n\n#### DUMP PER TAG ####");
			for (final Iterator it = repository.getSymbolicNames().iterator(); it.hasNext();) {
				final SymbolicName symbol = (SymbolicName) it.next();
				SvnConfigurationOptions.getTaskLogger().info("\nTAG: " + symbol.getName() + " / " + symbol.getDate());
				int loc = 0;
				for (final Iterator rev = symbol.getRevisions().iterator(); rev.hasNext();) {
					final Revision revision = (Revision) rev.next();
					SvnConfigurationOptions.getTaskLogger().info(
					        "  LOC:" + padIntRight(revision.getLines(), WIDTH_FOR_NUMBER) + " Rev:" + padRight(revision.getRevisionNumber(), WIDTH_FOR_NUMBER)
					                + " File: " + revision.getFile().getFilenameWithPath() + " dead:" + revision.isDead());
					loc += revision.getLines();
				}
				SvnConfigurationOptions.getTaskLogger().info("Total LOC: " + loc);
			}
		}
	}

	private void dumpPerFile(final SortedSet files) {
		totalCurrentLOCPerFile = 0;
		totalNumRevision = 0;
		int fileNumber = 0;
		totalMisMatch = 0;
		numberMisMatch = 0;
		for (final Iterator it = files.iterator(); it.hasNext();) {
			final VersionedFile rev = (VersionedFile) it.next();
			totalCurrentLOCPerFile += rev.getCurrentLinesOfCode();
			totalNumRevision += rev.getRevisions().size();
			SvnConfigurationOptions.getTaskLogger().info("File " + ++fileNumber + "/ " + rev.getFilenameWithPath() + " \tLOC:" + rev.getCurrentLinesOfCode());
			int sumDelta = 0;
			// go through all revisions for this file.
			for (final Iterator it2 = rev.getRevisions().iterator(); it2.hasNext();) {
				final Revision revi = (Revision) it2.next();

				sumDelta += revi.getLinesDelta();
				if (revi.isBeginOfLog()) {
					sumDelta += revi.getLines();
				}
				SvnConfigurationOptions.getTaskLogger()
				        .info(
				                "\tRevision:" + padRight(revi.getRevisionNumber(), WIDTH_FOR_NUMBER) + " \tDelta:"
				                        + padIntRight(revi.getLinesDelta(), WIDTH_FOR_NUMBER) + "\tLines:" + padIntRight(revi.getLines(), WIDTH_FOR_NUMBER)
				                        + "\t" + printBoolean("Ini:", revi.isInitialRevision()) + "\t" + printBoolean("BegLog", revi.isBeginOfLog()) + "\t"
				                        + printBoolean("Dead", revi.isDead()) + "\tSumDelta:" + padIntRight(sumDelta, WIDTH_FOR_NUMBER) + " "
				                        + revi.getSymbolicNames());
			}
			if (sumDelta != rev.getCurrentLinesOfCode()) {
				SvnConfigurationOptions.getTaskLogger().info(
				        "\t~~~~~SUM DELTA DOES NOT MATCH LOC " + rev.getCurrentLinesOfCode() + " vs " + sumDelta + " Diff:"
				                + (rev.getCurrentLinesOfCode() - sumDelta) + " ~~~~~~");
				totalMisMatch += (rev.getCurrentLinesOfCode() - sumDelta);
				numberMisMatch++;
			}
		}
	}

	private Set dumpPerRevision(final SortedSet revisions) {
		totalDelta = 0;
		final Set filesViaRevisions = new HashSet();
		totalLastRev = 0;
		SvnConfigurationOptions.getTaskLogger().info("\n\n#### DUMP PER REVISION ####");
		String previousRevision = "";
		int revTotal = -1;
		for (final Iterator it = revisions.iterator(); it.hasNext();) {
			final Revision rev = (Revision) it.next();
			if (!rev.getRevisionNumber().equals(previousRevision)) {
				previousRevision = rev.getRevisionNumber();
				if (revTotal != -1) {
					SvnConfigurationOptions.getTaskLogger().info("Total for this rev: " + totalDelta);
				}
				SvnConfigurationOptions.getTaskLogger().info(
				        "Revision " + padRight(rev.getRevisionNumber(), WIDTH_FOR_NUMBER) + " " + SDF.format(rev.getDate()));
			}
			SvnConfigurationOptions.getTaskLogger().info(
			        "\tlines:" + padIntRight(rev.getLines(), WIDTH_FOR_NUMBER) + " D:" + padIntRight(rev.getLinesDelta(), WIDTH_FOR_NUMBER) + " Rep:"
			                + padIntRight(rev.getReplacedLines(), WIDTH_FOR_NUMBER) + " New:" + padIntRight(rev.getNewLines(), WIDTH_FOR_NUMBER)
			                + printBoolean(" Initial", rev.isInitialRevision()) + printBoolean(" BegLog", rev.isBeginOfLog())
			                + printBoolean(" Dead", rev.isDead()) + " " + rev.getFile().getFilenameWithPath() + " tags:" + rev.getSymbolicNames());

			totalDelta += rev.getLinesDelta();
			if (rev.isBeginOfLog()) {
				totalDelta += rev.getLines();
			}
			revTotal = totalDelta;
			final VersionedFile file = rev.getFile();
			final Revision fileRev = file.getLatestRevision();
			if (!fileRev.isDead() /*
											 * &&
											 * fileRev.getRevisionNumber().equals(rev.getRevisionNumber())
											 */&& !filesViaRevisions.contains(file.getFilenameWithPath())) {
				totalLastRev += file.getCurrentLinesOfCode();
			}
			filesViaRevisions.add(file.getFilenameWithPath());
		}
		SvnConfigurationOptions.getTaskLogger().info("Total for this rev: " + totalDelta);
		return filesViaRevisions;
	}

	private String padIntRight(final int str, final int size) {
		return padRight(String.valueOf(str), size);
	}

	private String printBoolean(final String title, final boolean test) {
		return title + ":" + (test ? "Y" : "N");
	}

	private String padRight(final String str, final int size) {
		final StringBuffer buf = new StringBuffer(str);
		int requiredPadding = size;
		if (str != null) {
			requiredPadding = requiredPadding - str.length();
		}
		if (requiredPadding > 0) {
			for (int i = 0; i < requiredPadding; i++) {
				buf.append(" ");
			}
		}
		return buf.toString();
	}
}

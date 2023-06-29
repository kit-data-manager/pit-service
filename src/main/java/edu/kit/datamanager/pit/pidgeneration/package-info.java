/**
 * Generators for PID suffixes.
 * 
 * {@link PidSuffixGenerator}s generate {@link PidSuffix}es. Some PID generators
 * simply generate random PID, others take the {@link PidSuffix} from another
 * generator as input to modify it. A simple example is
 * {@link edu.kit.datamanager.pit.pidgeneration.generators.PidSuffixGenLowerCase},
 * which outputs the same PID it got from its internal generator, but in lower
 * case.
 * 
 * <p>
 * <img src="doc-files/architecture.drawio.svg" alt="UML diagram of
 * architecture">
 * </p>
 * 
 * The benefit of this architecture is that it is easy to add and combine new
 * PID generators. The following example shows the possibilities:
 * 
 * <p>
 * <img src="doc-files/visualization.drawio.svg" alt="explanation">
 * </p>
 */
package edu.kit.datamanager.pit.pidgeneration;

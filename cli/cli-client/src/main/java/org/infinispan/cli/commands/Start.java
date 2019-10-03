package org.infinispan.cli.commands;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandResult;
import org.infinispan.cli.activators.DisabledActivator;
import org.infinispan.cli.impl.ContextAwareCommandInvocation;
import org.kohsuke.MetaInfServices;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 10.0
 **/
@MetaInfServices(Command.class)
@CommandDefinition(name = Start.CMD, description = "Starts a batch", activator = DisabledActivator.class)
public class Start extends CliCommand {

   public static final String CMD = "start";

   @Override
   public CommandResult exec(ContextAwareCommandInvocation invocation) {
      return CommandResult.SUCCESS;
   }

   @Override
   public int nesting() {
      return 1;
   }
}

#! /usr/local/bin/perl

package Toolkit;
use strict;
use warnings;
use FileHandle;
use Getopt::Long;

# http://crawdad.org/unimi/pmtr/
#id_source id_destination t_start_contact t_end_contact

#change the names of the files
my $inputFile = 'pmtr.txt';
my $outFileName = 'pmtrOut.txt';
#change the names of the files

my $inFh = new FileHandle;
my $outFh = new FileHandle;
$inFh->open("<$inputFile") or die "Can't open input file $inputFile";
$outFh->open(">$outFileName") or die "Can't create outputfile $outFileName";

print $outFh "# Connection trace file for the ONE. Converted from $inputFile \n";

my @lines = <$inFh>; # read whole file to array
my @output;
my $nextNodeId = 0;
my ($nodeId1, $nodeId2, %nodeIds);
my $startTime = 30198;

foreach (@lines) {
  if (m/^\s$/ or m/^#/) {
    next; # skip empty lines
  }
  
  my ($node1, $node2, $start, $end) = 
    m/(\d+) (\d+) (\d+) (\d+)/;
  die "Invalid input line: $_" unless ($node1 and $node2);

  if ($start < $startTime) {
	print "$start \n";
  }
  # map node IDs consistently to network addresses
  if (exists $nodeIds{$node1}) {
    $nodeId1 = $nodeIds{$node1};
  }
  else {
    $nodeId1 = $nextNodeId;
    $nodeIds{$node1} = $nextNodeId;
    $nextNodeId++;
  }
  if (exists $nodeIds{$node2}) {
    $nodeId2 = $nodeIds{$node2};
  }
  else {
    $nodeId2 = $nextNodeId;
    $nodeIds{$node2} = $nextNodeId;
    $nextNodeId++;
  }
  
  if ($end - $start > 0) {
	my $s = $start-$startTime;
	my $e = $end-$startTime;
	push(@output, "$s CONN $nodeId1 $nodeId2 up");
	push(@output, "$e CONN $nodeId1 $nodeId2 down");
  }
}

# sort result by time stamp
@output = sort 
{
  my ($t1) = $a =~ m/^(\d+)/;
  my ($t2) = $b =~ m/^(\d+)/;
  $t1 <=> $t2;
} @output;


# print all the result lines to output file
print $outFh join("\n", @output);

print "Node name to network ID mapping:\n";
while (my ($k,$v) = each %nodeIds ) {
    print "$k => $v\n";
}

$outFh->close();
$inFh->close();
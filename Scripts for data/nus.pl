#! /usr/local/bin/perl

package Toolkit;
use strict;
use warnings;
use FileHandle;
use Getopt::Long;

# http://crawdad.cs.dartmouth.edu/nus/contact/

# This script limits the number of contacts and connections between them
# because they are too many and impossible to simulate!! Change to your desire!

#change the names of the files
my $inputFile = 'mobicom06-trace.txt';
my $outFile = 'mobicom06-traceoutNEW.txt';
#change the names of the files

my $inFh = new FileHandle;
my $outFh = new FileHandle;
$inFh->open("<$inputFile") or die "Can't open input file $inputFile";
$outFh->open(">$outFile") or die "Can't create outputfile $outFile";

my @lines = <$inFh>; # read whole file to array
print "done reading...\n";
my @output;
my $linenum = 0;
my ($start, $sid, $studs, $duration, @students);
my $dupSize;
my %toRem;
my $nextNodeId = 0;
my ($nodeId1, $nodeId2, %nodeIds, %curConn, $conEndTime, $startTime);

#change the numbers as you wish
my $maxStud = 800;
my $conCoun = 3;

foreach (@lines) {
	if ((m/^\s$/) || ($linenum == 0)){
		$linenum++;
		next; # skip empty lines and first line (total #sessions, total #students)
	}
	#format
	#start time of session in business hours starting from 0, session id, #students in session, duration of sessions in hours
	#students ids

	if ($linenum % 2 == 1) {
		($start, $sid, $studs, $duration) = 
			m/(\d+)\s+(\d+)\s+(\d+)\s+(\d+)/;
		#print join(" ", $start, $sid, $studs, $duration, "\n");
		#die "Invalid input line: $_" unless ($start);
	}
	else {
		@students = /(\w+)/g; 
		#print join (" ", @students, "\n");
		my $size = @students;
		my $curstud = 0;
	}
	
	$linenum++;
}

$linenum = 0;
foreach (@lines) {
	if ((m/^\s$/) || ($linenum == 0)){
		$linenum++;
		next; # skip empty lines and first line (total #sessions, total #students)
	}
	#format
	#start time of session in business hours starting from 0, session id, #students in session, duration of sessions in hours
	#students ids

	if ($linenum % 2 == 1) {
		($start, $sid, $studs, $duration) = 
			m/(\d+)\s+(\d+)\s+(\d+)\s+(\d+)/;
		print join(" ", $start, $sid, $studs, $duration, "\n");
		#die "Invalid input line: $_" unless ($start);
	}
	else {
		@students = /(\w+)/g; 
		#print join (" ", @students, "\n");
		my $size = @students;
		my $curstud = 0;
		my $curs = keys %nodeIds;
		for my $i (0..$size-1) {
			if ($students[$i] < $maxStud) {
				if (exists $nodeIds{$students[$i]}) {
					$nodeId1 = $nodeIds{$students[$i]};
				}
				else {
					$nodeId1 = $nextNodeId;
					$nodeIds{$students[$i]} = $nextNodeId;
					$nextNodeId++;
				}
			
				my $p1;
				my $tr = 2;
				my $cnn = 0;
				my $curno = 1;
				
				#print "size = $size\n";
				while ($cnn < $conCoun && $tr < $size) {
					#print "tr = $tr\n";
					if ($i+$curno < $size) {
						$p1 = $i+$curno;
						$curno++;
					}
					else {
						$p1 = $conCoun - $cnn - 1;
					}
				
					#print "\tstudent j: $students[$p1] \n";
				
					if ($students[$p1] < $maxStud) {
						if (exists $nodeIds{$students[$p1]}) {
							$nodeId2 = $nodeIds{$students[$p1]};
						}
						else {
							$nodeId2 = $nextNodeId;
							$nodeIds{$students[$p1]} = $nextNodeId;
							$nextNodeId++;
						}
				
						$startTime = $start * 60 * 60;
						$conEndTime = $startTime + $duration * 60 * 60;
				
						keys %curConn;
						while (my ($k, $v) = each %curConn){
							#print "curConn: $k \t $v \n";
							if ($v < $startTime) {
								my @toks = split(/_/, $k);
								push(@output, "$v CONN $toks[0] $toks[1] down");
								delete $curConn{$k};
							}
						}

						#print "nodeId2 = $nodeId2 \n";
						my $temp1 = $nodeId1 . "_" . $nodeId2;
						my $temp2 = $nodeId2 . "_" . $nodeId1;
						if (exists $curConn{$temp1}) {
							if ($startTime <= $curConn{$temp1}) {
								$curConn{$temp1} = $conEndTime;
							}
						}
						elsif (exists $curConn{$temp2}) {
							if ($startTime <= $curConn{$temp2}) {
								$curConn{$temp2} = $conEndTime;
							}
						}
						else {
							$curConn{$temp1} = $conEndTime;
							push(@output, "$startTime CONN $nodeId1 $nodeId2 up");
						}
						$cnn++;
					}
					$tr++;					
				}
			}
		}
	}
	$linenum++;
}

while (my ($k, $v) = each %curConn){
	my @toks = split(/_/, $k);
	push(@output, "$v CONN $toks[0] $toks[1] down");
	delete $curConn{$k};
}

my $s2 = keys %nodeIds;
print "nodes total: $s2\n";

# sort result by time stamp
@output = sort 
{
  my ($t1) = $a =~ m/^(\d+)/;
  my ($t2) = $b =~ m/^(\d+)/;
  $t1 <=> $t2;
} @output;

# print all the result lines to output file
print $outFh join("\n", @output);

$outFh->close();
$inFh->close();
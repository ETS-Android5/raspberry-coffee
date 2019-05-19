# Matrix and System resolution

import math
import datetime

debug = False

def initSqMatrix(dim):
	mat = []
	# Initialize
	for r in range(dim):
		line = []
		for c in range(dim):
			line.append(0)
		mat.append(line)
	return mat

# Minor matrix
def minor(matrix, row, col):
	small = []
	dim = len(matrix) # Assume square matrix
	for c in range(dim):
		if c != col:
			smallLine = []
			for r in range(dim):
				if r != row:
					smallLine.append(matrix[r][c])
			small.append(smallLine)
	return small

# Transposed matrix
def transposed(matrix):
	# Initialize
	trans = initSqMatrix(len(matrix))
	# fill it up
	for r in range(len(matrix)):
		for c in range(len(matrix)):
			trans[r][c] = matrix[c][r]

	if debug:
		print "Transposed:"
		print_matrix(trans)

	return trans

# Comatrix
def comatrix(matrix):
	# Initialize
	comat = initSqMatrix(len(matrix))
	for r in range(len(matrix)):
		for c in range(len(matrix)):
			comat[r][c] = determin(minor(matrix, r, c)) * math.pow((-1), (r + c + 2))

	if debug:
		print "Comatrix:"
		print_matrix(comat)

	return comat

# Determinant of a square matrix
def determin(matrix):
	value = 0.0
	if len(matrix) == 1:
		value = matrix[0][0]
	else:
		for C in range(len(matrix)): # assume square matrix
			minorValue = determin(minor(matrix, 0, C))
			value += (matrix[0][C] * minorValue * math.pow(-1.0, C + 1 + 1)) # line C, col 1

	if debug:
		print "Determinant of"
		print_matrix(matrix)
		print " is %f" % value

	return value

def multiply(matrix, n):
	result = initSqMatrix(len(matrix))
	for r in range(len(matrix)):
		for c in range(len(matrix)):
			result[r][c] = matrix[r][c] * n
	return result

def invert(matrix):
	return multiply(transposed(comatrix(matrix)), (1.0 / determin(matrix)))

def print_matrix(matrix):
	for r in range(len(matrix)):
		line = "| "
		for c in range(len(matrix)):
			line += ("%f " % matrix[r][c])
		line += " |"
		print line

#
# System functions
#

# Solves a system, n equations, n unknowns.
# <p>
# the values we look for are x, y, z.
# <pre>
# ax + by + cz = X
# Ax + By + Cz = Y
# Px + Qy + Rz = Z
# </pre>
# @param m Coeffs matrix, n x n (left) from the system above
# <pre>
# | a b c |
# | A B C |
# | P Q R |
# </pre>
# @param c Constants array, n (right) <code>[X, Y, Z]</code> from the system above
# @return the unknown array, n. <code>[x, y, z]</code> from the system above
#
def solve_system(matrix, cst):
	result = []
	for r in range(len(matrix)): # init
		result.append(0)
	inverted = invert(matrix)

	if debug:
		# Print inverted matrix
		print "Inverted:"
		print_matrix(inverted)

	for r in range(len(matrix)):
		result[r] = 0
		for c in range(len(matrix)):
			result[r] += (inverted[r][c] * cst[c])
	return result

def print_system(matrix, constants):
	unknowns = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
	dim = len(matrix)
	for r in range(dim):
		line = ""
		for c in range(dim):
			if len(line) > 0:
				line += " + "
			line += ("(%f x %c)" % (matrix[r][c], unknowns[c]))
		line += (" = %f" % constants[r])
		print line

#
#
# Main part below (actual execution), using the above.

matrix = [
	[ 12,       13,      14 ],
	[  1.345, -654,       0.001 ],
	[ 23.09,     5.3, -12.34 ]
]
constants = [ 234, 98.87, 9.876 ]

#print matrix
#min = minor(matrix, 0, 0)
#print min
#print determin(min)
#print transposed(matrix)

#print invert(matrix)

print "Solving:"
print_system(matrix, constants)

before = datetime.datetime.now()
result = solve_system(matrix, constants)
after = datetime.datetime.now()
print "Done in ", (after - before).seconds, "s :", (after - before).microseconds, ("\u03bc".decode('unicode-escape') + "s")

print "A = ", result[0]
print "B = ", result[1]
print "C = ", result[2]
print ""
# Proof:
x = (matrix[0][0] * result[0]) + (matrix[0][1] * result[1]) + (matrix[0][2] * result[2])
y = (matrix[1][0] * result[0]) + (matrix[1][1] * result[1]) + (matrix[1][2] * result[2])
z = (matrix[2][0] * result[0]) + (matrix[2][1] * result[1]) + (matrix[2][2] * result[2])
print "Proof X:", x
print "Proof Y:", y
print "Proof Z:", z

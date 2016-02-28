function extReport (results, data) {
	var len = results.length,
		str = '',
		error, globals, unuseds;
	results.forEach(function (result) {
		error = result.error;
		str += 'line ' + error.line + ', col ' +
			error.character + ', ' + error.reason + '\n';
	});
	str += len > 0 ? ("\n" + len + ' error' + ((len === 1) ? '' : 's')) : "";
	Object.keys(data).forEach(function (data) {
		globals = data.implieds;
		unuseds = data.unused;
		if (globals || unuseds)
			str += '\n';
		if (globals) {
			str += '\tImplied globals:\n';
			globals.forEach(function (global) {
				str += '\t\t' + global.name  + ': ' + global.line + '\n';
			});
		}
		if (unuseds) {
			str += '\tUnused Variables:\n\t\t';
			unuseds.forEach(function (unused) {
				str += unused.name + '(' + unused.line + '), ';
			});
		}
	});
	if (str)
		str += "\n";
	return str;
}

function jshintReporter(data, options) {
	try {
		
		var str = '',
			results = [],
			lines=[],
			len,
			error,
			errors = data.errors;

		if (options.oneErrorPerLine === undefined)
			options.oneErrorPerLine = true;
		if (errors && errors.length && errors.length > 0) {
			if (options.oneErrorPerLine)
				str += 'reporting one error per line\n';
			str += '\n';
			errors.forEach(function (error) {
				if (error && (!(options.oneErrorPerLine) || (options.oneErrorPerLine && lines.indexOf(error.line) === -1))) {
					lines.push(error.line);
					results.push({error: error});
				}
			});
		}
		if (options.extendedReport)
			str = extReport(results, data);
		else {
			
			len = results.length;
			results.forEach(function (result) {
				error = result.error;
				str += 'line ' + error.line + ', col ' +	error.character + ', ' + error.reason + '\n';
			});
			
				str += "\n" + len + ' error' + ((len === 1) ? '' : 's total, ' + lines.length + ' errors shown') + "\n";

		}
		return str;
	}
	catch (e) {
		throw e;
	}
}
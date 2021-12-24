const verifySMS = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/verifySMS/`, baseUrl);
	return fetch(url.toString(), {
		method: 'POST', 
		headers : new Headers({
 			'Content-Type': 'application/json'
		}),
		body: JSON.stringify({
			otp : parameters.otp,
			to : parameters.to
		})
	});
}

const verifySMSForm = (container) => {
	const html = `<form id='verifySMS-form'>
		<div id='verifySMS-otp-form-field'>
			<label for='otp'>otp</label>
			<input type='text' id='verifySMS-otp-param' name='otp'/>
		</div>
		<div id='verifySMS-to-form-field'>
			<label for='to'>to</label>
			<input type='text' id='verifySMS-to-param' name='to'/>
		</div>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)

	const otp = container.querySelector('#verifySMS-otp-param');
	const to = container.querySelector('#verifySMS-to-param');

	container.querySelector('#verifySMS-form button').onclick = () => {
		const params = {
			otp : otp.value !== "" ? otp.value : undefined,
			to : to.value !== "" ? to.value : undefined
		};

		verifySMS(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { verifySMS, verifySMSForm };
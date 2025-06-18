const axios = require('axios');

export default async function handler(req, res) {
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Hanya POST yang diizinkan' });
  }

  const { user_name, user_email, user_role } = req.body;

  try {
    const response = await axios.post('https://api.emailjs.com/api/v1.0/email/send', {
      service_id: 'service_3zotwxq',
      template_id: 'template_urytd4h',
      user_id: 'NO3Oi2Oej2xFNUHr_',
      template_params: {
        user_name,
        user_email,
        user_role,
        message: user_role === 'teacher'
          ? 'Seorang guru baru telah mendaftar dan memerlukan verifikasi'
          : 'Seorang orang tua baru telah mendaftar dan memerlukan verifikasi'
      }
    });

    return res.status(200).json({ success: true, response: response.data });
  } catch (error) {
    return res.status(500).json({ success: false, error: error.message });
  }
}
